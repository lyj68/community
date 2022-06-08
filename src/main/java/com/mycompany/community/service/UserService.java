package com.mycompany.community.service;

import com.mycompany.community.dao.UserMapper;
import com.mycompany.community.entity.LoginTicket;
import com.mycompany.community.entity.User;
import com.mycompany.community.util.CommunityConstant;
import com.mycompany.community.util.CommunityUtil;
import com.mycompany.community.util.MailClient;
import com.mycompany.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //将环境变量写在配置文件中,让他根据运行的环境进行读取
    //常量注入
    @Value("${community.path.domain}")
    String domain;

    @Value("${server.servlet.context-path}")
    String contextPath;


    public User findUserById(int id) {
        User user = getCache(id);
        if (user==null){
            user = initCache(id);
        }
        return user;
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();

        //空值处理
        if (user == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        //验证账号是否存在
        User u = userMapper.selectByName(user.getUsername());
        if (u != null){
            map.put("usernameMsg","该账户已存在");
            return map;
        }

        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户：把信息存入库中
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101(userid)/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);


        return map;
    }

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    //expiredSeconds秒，有效表示持续多少秒
    public Map<String,Object> login(String username,String password , int expiredSeconds){
        Map<String, Object> map = new HashMap<>();

        //验证空值
        if (StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //验证账号是否存在
        User user = userMapper.selectByName(username);
        if (user == null){
            map.put("usernameMsg","账号不存在");
            return map;
        }

        //验证账号是否激活过
        if (user.getStatus()==0){
            map.put("usernameMsg","该账号未激活!");
            return map;
        }

        //验证密码，客户端传来的是明文密码，需要加密之后和库里验证
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("passwordMsg","密码不正确");
            return map;
        }

        //登录信息验证成功之后，给他生成一个凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));

        // 把ticket插入到redis中
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey,loginTicket);

        //loginTicketMapper.insertLoginTicket(loginTicket);


        //把凭证返回
        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    //设置退出设置登录凭证状态为1
    public void logout(String ticket){
//        loginTicketMapper.updatStatus(ticket,1);
//        return;
        // 取出凭证，并且修改状态再放回去
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,loginTicket);

    }

    public LoginTicket findLoginTicket(String ticket){
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        return loginTicket;
    }

    public int updateHeader(int userId, String headerUrl){
        int row = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return row;
    }

    public int updatePassword(int userId,String password){
        int row = userMapper.updatePassword(userId,password);
        clearCache(userId);
        return row;
    }

    public User findUserByName(String name) {
        return userMapper.selectByName(name);
    }

    // 1.优先从缓存中取值
    public User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    // 2.取不到时初始化缓存数据
    public User initCache(int userId){
        // 先从mysql中取出user
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
        return user;

    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    // 根据用户获取对应权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }


}
