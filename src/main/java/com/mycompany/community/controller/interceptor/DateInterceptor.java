package com.mycompany.community.controller.interceptor;

import com.mycompany.community.service.DateService;
import com.mycompany.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DateInterceptor implements HandlerInterceptor {

    @Autowired
    private DateService dateService;

    @Autowired
    private HostHolder hostHolder;

    // 在请求前计入数据
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 计入uv
        String ip = request.getRemoteHost();
        dateService.recordUV(ip);

        // 计入dau
        if (hostHolder.getUser()!=null) {
            int id = hostHolder.getUser().getId();
            dateService.recordDAU(id);
        }

        return true;
    }
}
