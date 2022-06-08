package com.mycompany.community.controller;

import com.mycompany.community.entity.Comment;
import com.mycompany.community.entity.DiscussPost;
import com.mycompany.community.entity.Event;
import com.mycompany.community.event.EventProducer;
import com.mycompany.community.service.CommentService;
import com.mycompany.community.service.DiscussPostService;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.HostHolder;
import com.mycompany.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping(path = "/comment")
public class  CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        //添加
        commentService.insertComment(comment);

        // 触发事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);

        // 给哪个用户了
        // 如果是给帖子评论了，那对象肯定是发帖子的用户
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(discussPostId);
            event.setEntityUserId(target.getUserId());
        }else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            // 如果是给评论评论的话，那对象肯定是评论的用户
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        // 加入生成者队列
        eventProducer.fireEvent(event);

        if (comment.getEntityType()== ENTITY_TYPE_POST){
            // 增加帖子的评论也需要触发事件（相当于修改帖子）
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            // 把事件给生产者
            eventProducer.fireEvent(event);

            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }


        //重定向到详情页
        return "redirect:/discuss/detail/" + discussPostId;

    }

}
