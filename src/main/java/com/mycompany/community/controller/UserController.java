package com.mycompany.community.controller;

import com.mycompany.community.annotation.LoginRequired;
import com.mycompany.community.entity.User;
import com.mycompany.community.service.FollowService;
import com.mycompany.community.service.LikeService;
import com.mycompany.community.service.UserService;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import com.mycompany.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping(path = "/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.domain}")
    private String domain;

    @Value("${community.path.upload}")
    private String upload;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSetting(Model model){
        // 生成上传凭证

        // 1. 上传文件名
        String fileName = CommunityUtil.generateUUID();

        // 2.设置响应信息：需要使用七牛云指定的StringMap格式
        StringMap policy = new StringMap();
        // 设置响应体为json格式：code:0
        policy.put("returnBody",CommunityUtil.getJsonString(0));

        // 3.生成凭证
        Auth auth = Auth.create(accessKey,secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // 更新头像路径
    // 编程异步方式了
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJsonString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJsonString(0);
    }

    // 弃用
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        //获取图片名
        String filename = headerImage.getOriginalFilename();
        //获取后缀
        String suffix = filename.substring(filename.lastIndexOf('.'));
        //判断图片后缀是否合法
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件格式不正确!");
            return "/site/setting";
        }

        //在本地重新生成图片保存路径，以便于保存
        // 生成随机文件名
        filename = CommunityUtil.generateUUID()  + suffix;
        // 确定文件存放的路径
        File dest = new File(upload + "/" + filename);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)，不能用本地路径
        // http://localhost:8080/community/user/header/xxx.png
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        //更新库里面的用户头像路径
        User user = hostHolder.getUser();
        userService.updateHeader(user.getId(),headerUrl);

        //更新头像成功后，重定向到index
        return "redirect:/index";

    }


    // 弃用
    // 获取图片
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        // 修改文件名字
        // 改成 服务器存放路径
        filename = upload + "/" + filename;

        //获取后缀
        String suffix = filename.substring(filename.lastIndexOf('.'));

        // 设置图片
        // 响应图片
        response.setContentType("image/"+suffix);
        try (
                //获取图片二进制流
                FileInputStream fis = new FileInputStream(filename);
                //获取二进制输出流
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            //从输入流读出写入输出流
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }

    }

    @LoginRequired
    @RequestMapping(path = "/upatePsw",method = {RequestMethod.POST,RequestMethod.GET})
    public String updatePsw(String passwordCheck,Model model, String passwordOld,String passwordNew,@CookieValue("ticket") String ticket){

        if (passwordOld==null){
            model.addAttribute("oldPasswordMsg", "旧密码尚未输入!");
            return "/site/setting";
        }

        if (passwordNew==null){
            model.addAttribute("newPasswordMsg", "新密码尚未输入!");
            return "/site/setting";
        }

        if (passwordCheck==null){
            model.addAttribute("CheckPasswordMsg", "请重新输入确认密码!");
            return "/site/setting";
        }

        if (!passwordCheck.equals(passwordNew)){
            model.addAttribute("CheckPasswordMsg", "密码不一致!");
            return "/site/setting";
        }

        User user = hostHolder.getUser();

        //核验原密码对不对
        String pswOld = user.getPassword();
        passwordOld = CommunityUtil.md5(passwordOld + user.getSalt());
        if (!pswOld.equals(passwordOld)){
            model.addAttribute("oldPasswordMsg","原密码不正确，请重新输入");
            return "/site/setting";
        }

        //处理新密码，明文密码加盐
        passwordCheck = CommunityUtil.md5(passwordCheck + user.getSalt());

        userService.updatePassword(user.getId(),passwordCheck);

        userService.logout(ticket);

        return "redirect:/login";


    }

    // 获取个人主页
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        // 查找用户
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户给前端
        model.addAttribute("user",user);

        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 用户的关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 用户的粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 当前登录的用户是否已关注这个用户
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";


    }


}
