package com.mycompany.community.controller;

import com.mycompany.community.entity.Comment;
import com.mycompany.community.entity.DiscussPost;
import com.mycompany.community.entity.Page;
import com.mycompany.community.entity.User;
import com.mycompany.community.service.CommentService;
import com.mycompany.community.service.DiscussPostService;
import com.mycompany.community.service.LikeService;
import com.mycompany.community.service.UserService;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import com.mycompany.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    //返回json字符串
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    //浏览器提供内容
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser();
        if (user == null){
            return CommunityUtil.getJsonString(403,"你还没有登录哦!");
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());

        //调用业务层处理discusspost对象
        discussPostService.addDiscussPost(discussPost);


        return CommunityUtil.getJsonString(0,"发布成功!");

    }

    @RequestMapping(path = "/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        // 帖子获取
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        // 发送给模板
        model.addAttribute("post",post);
        // 通过帖子的userid获取作者信息user
        User user = userService.findUserById(post.getUserId());
        // 发送给模板
        model.addAttribute("user",user);

        // 帖子点赞数目
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount",likeCount);

        // 点赞状态
        // 没登录就显示不可能有"已赞“，所以返回0
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        // 评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());
        
        //获取帖子评论集合
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        // 根据评论查询信息返回给模板 : 评论VO列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if(commentList!=null){
            for (Comment comment: commentList) {
                HashMap<String, Object> commentVo = new HashMap<>();
                //返回评论
                commentVo.put("comment",comment);
                //返回评论的用户信息
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                // 点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);
                //获取帖子的回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                // 根据回复查询信息返回给模板 : 回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList!=null){
                    //遍历帖子列表
                    for (Comment reply: replyList) {
                        //把回复信息放在map中返回
                        HashMap<String, Object> replyVo = new HashMap<>();
                        replyVo.put("reply",reply);
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        // 看回复是否有目标回事是针对某个评论的
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);
                        replyVoList.add(replyVo);

                    }
                }
                //把查询到的帖子评论的回复放入帖子map中
                commentVo.put("replys", replyVoList);

                // 查询回复的数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);

                commentVoList.add(commentVo);
            }

        }

        //最终给model
        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";

    }

}
