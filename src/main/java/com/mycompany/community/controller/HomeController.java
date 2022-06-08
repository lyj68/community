package com.mycompany.community.controller;

import com.mycompany.community.entity.DiscussPost;
import com.mycompany.community.entity.Page;
import com.mycompany.community.entity.User;
import com.mycompany.community.service.DiscussPostService;
import com.mycompany.community.service.LikeService;
import com.mycompany.community.service.UserService;
import com.mycompany.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        //方法调用前，springmvc会自动实例化Model和Page，并将Page注入Model
        //所以，在thymeleaf中可以直接访问Page对象中的数据
        page.setRows(discussPostService.findDiscussPostRows(0));
        // 为了保证分页任然能够有排序信息
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0,page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);
                discussPosts.add(map);
            }
        }
        //往model里面装数据
        model.addAttribute("discussPosts", discussPosts);
        // 前端显示需要用
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }

    // 拒绝访问时的提示页面
    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    // 拒绝访问时的提示页面
    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }


}
