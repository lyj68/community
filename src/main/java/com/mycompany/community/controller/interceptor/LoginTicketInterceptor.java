package com.mycompany.community.controller.interceptor;

import com.mycompany.community.entity.LoginTicket;
import com.mycompany.community.entity.User;
import com.mycompany.community.service.UserService;
import com.mycompany.community.util.CookieUtil;
import com.mycompany.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;


    //请求之前先获取user
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从cookie中获取ticket
        String ticket = CookieUtil.getCookie(request, "ticket");
        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证是否有效:验证状态和过期时间
            if (loginTicket != null && loginTicket.getStatus()==0 && loginTicket.getExpired().after(new Date())){
                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                //把user存入线程池hostholder
                // 在本次请求线程中持有用户
                hostHolder.setUser(user);
                // 构建用户认证的结果,并存入SecurityContext,以便于Security进行授权.(因为未采用security自带的认证需要绕开，但需要自己提供授权需要的token)
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    //在template之前，把user送给template给前端使用
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        //请求结束的时候。清除SecurityContext
        SecurityContextHolder.clearContext();
    }
}
