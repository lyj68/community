package com.mycompany.community.controller;

import com.mycompany.community.annotation.LoginRequired;
import com.mycompany.community.entity.User;
import com.mycompany.community.service.LikeService;
import com.mycompany.community.util.CommunityUtil;
import com.mycompany.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

@Controller
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @LoginRequired
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId){
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

        return CommunityUtil.getJsonString(0,map);


    }

}
