package com.mycompany.community.controller;

import com.google.code.kaptcha.Producer;
import com.mycompany.community.entity.User;
import com.mycompany.community.service.UserService;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import com.mycompany.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private UserService userService;

    //把配置文件里面的属性注入
    //常量注入
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;


    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /*  在controller类中返回到页面中一共有两种方式，使用thymeleaf模板引擎的方式和不使用模板的方式（即controller的返回值为ModelAndView或者String）。
    在controller类中返回值为ModelAndView或者String，二者的区别就在于ModelAndView能够像session一样存储一些属性。  */

    //处理访问注册页面的方法，获取注册页面，返回模板（View）路径
    // @RequestMapping 中的 value 和 path 属性，这两个属性作用相同，可以互换
    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String getRegisterPage(){
        //添加thymeleaf依赖后，默认根路径改为templates
        //不使用thymeleaf时候，默认根路径为static
        return "/site/register";
    }

    //给浏览器返回一个html
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }


    //通过它来指定控制器可以处理哪些URL请求
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            //往前台传数据，可以传对象，可以传List
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }

    }


    //@PathVariable从path中取参数
    //通过 @PathVariable 可以将URL中占位符参数{xxx}绑定到处理器类的方法形参中@PathVariable(“xxx“)
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        //session.setAttribute("kaptcha", text);

        // 验证码的归属
        // 生成的随机字符串，用来临时标识一下该用户，把他存放再cookie中标识用户
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        // 设置cook存活时间
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    //处理表单提交的数据，返回到服务端的库里（所以需要修改，是post不是get）.返回数据需要model,需要从sesion中取出验证码
    //登录成功，把ticket存入到cookie中给用户
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner) {
        //验证验证码是否正确
        //从session中获取kaptcha
        //String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;
        // 如果cookie里面的key还存活
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }

        //检查用户密码
        //先获取过期时间
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")){
            //登录成功
            //把ticket放在cookie里面给客户端保存
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            //设置cookie作用范围
            cookie.setPath(contextPath);
            //设置过期时间
            cookie.setMaxAge(expiredSeconds);
            //加入response中给客户端
            response.addCookie(cookie);
            //返回字符串可以重定向到一个url地址，新界面
            return "redirect:/index";
        }else {
            //登录失败
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login";
        }

    }

    //从cookie中获取ticket ，调用业务层退出方法,重定向到登录页面
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }


}
