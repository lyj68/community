package com.mycompany.community.controller.interceptor;

import com.mycompany.community.annotation.LoginRequired;
import com.mycompany.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    HostHolder hostHolder;

    //在执行之前拦截不给他进入
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // handler 拦截目标得是方法才行
        if (handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            //获取拦截方法
            Method method = handlerMethod.getMethod();
            // 看拦截方法是否有注解
            LoginRequired annotation = method.getAnnotation(LoginRequired.class);
            //如果是需要被拦截的方法，且未登录，则拒绝访问
            if (annotation!=null && hostHolder.getUser()==null){
                //拒绝方法重定向到登录界面
                response.sendRedirect(request.getContextPath()+"/login");
                return false;
            }

        }


        return true;
    }
}
