package com.mycompany.community.controller;

import com.mycompany.community.annotation.LoginRequired;
import com.mycompany.community.entity.Event;
import com.mycompany.community.entity.User;
import com.mycompany.community.event.EventProducer;
import com.mycompany.community.service.LikeService;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import com.mycompany.community.util.HostHolder;
import com.mycompany.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @LoginRequired
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId,int postId){
        User user = hostHolder.getUser();

        // 点赞
        likeService.like(user.getId(), entityType,entityId,entityUserId);

        // 获取点赞数目
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);

        // 获取点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        // 把信息封装成map，以转成json
        HashMap<String, Object> map = new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);

        // 触发点赞事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST){
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return CommunityUtil.getJsonString(0,map);


    }

}
