package com.mycompany.community.event;

import com.alibaba.fastjson.JSONObject;
import com.mycompany.community.entity.DiscussPost;
import com.mycompany.community.entity.Event;
import com.mycompany.community.entity.Message;
import com.mycompany.community.service.DiscussPostService;
import com.mycompany.community.service.ElasticsearchService;
import com.mycompany.community.service.MessageService;
import com.mycompany.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    // 消费发帖事件
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


}
