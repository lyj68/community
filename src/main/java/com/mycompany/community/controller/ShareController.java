package com.mycompany.community.controller;

import com.mycompany.community.entity.Event;
import com.mycompany.community.event.EventProducer;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String storage;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/share",method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl){
        // 文件名
        String fileName = CommunityUtil.generateUUID();

        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl",htmlUrl)
                .setData("fileName",fileName)
                .setData("suffix",".png");
        // 放入生产者队列中
        eventProducer.fireEvent(event);

        // 返回访问路径:以便获取生成的长图
        Map<String, Object> map = new HashMap<>();
//        map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
        map.put("shareUrl", shareBucketUrl + "/" + fileName);

        return CommunityUtil.getJsonString(0, null, map);
    }

    // 废弃
    // 获取长图：通过response体返回长图显示
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {

        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }

        // 设置文件格式
        response.setContentType("image/png");

        // 长图文件
        File file = new File(storage + "/" + fileName + ".png");

        try {
            // 要将长图文件用response输出，所以获取输出流
            OutputStream os = response.getOutputStream();
            // 长图文件作为输入流
            FileInputStream fis = new FileInputStream(file);
            // 文件缓冲区
            byte[] buffer = new byte[1024];
            // 文件偏移值
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("获取长图失败: " + e.getMessage());
        }
    }


}
