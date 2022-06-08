package com.mycompany.community.event;

import com.alibaba.fastjson.JSONObject;
import com.mycompany.community.entity.DiscussPost;
import com.mycompany.community.entity.Event;
import com.mycompany.community.entity.Message;
import com.mycompany.community.service.DiscussPostService;
import com.mycompany.community.service.ElasticsearchService;
import com.mycompany.community.service.MessageService;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        // 将事件解析为对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        // 发送站内通知
        // 将消息存入数据库中
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 消息内容用map封装
        HashMap<String, Object> content = new HashMap<>();
        // 触发事件的人
        content.put("userId", event.getUserId());
        // 触发的对象
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        // 剩余消息放入content中
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        // 将map转成json字符串存储
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

    }

    // 消费发帖事件：将发布的帖子存入ES中
    @KafkaListener(topics = TOPIC_PUBLISH)
    public void handlePublishMessage(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        // 将事件解析为对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        // 把帖子存入elasticsearch服务器里面
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);

    }

    // 消费分享长图事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        // 将事件解析为对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        // 从生产者事件中获取信息
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // 拼生成长图的命令
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 上传至七牛云：因为生成长图是异步的需要时间，所以需要定期任务去确定生成成功了，然后再在七牛云上传
        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
        uploadTask task = new uploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);

    }

    // 匿名类来实现定时任务
    class uploadTask implements Runnable{

        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值:方便结束 定时任务
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        // 强制构造器设置文件名，后缀，和开始时间
        public uploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 生成失败
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长,终止任务:" + fileName);
                // 结束定时任务
                future.cancel(true);
                return;
            }
            // 上传失败
            if (uploadTimes >= 5) {
                logger.error("上传次数过多,终止任务:" + fileName);
                // 结束定时任务
                future.cancel(true);
                return;
            }

            // 生成长图的本地文件路径
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 设置上传凭证
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJsonString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                // 指定上传机房地址
                UploadManager manager = new UploadManager(new Configuration(Zone.zone2()));
                try {
                    // 开始上传图片
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    // 处理响应结果: 将response转为json对象
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }





}
