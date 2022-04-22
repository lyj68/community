package com.mycompany.community.controller.Advice;


import com.mycompany.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

// 表示只加载含有controller注解的bean
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    // 加载日志
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);


    // 参数是获取所有异常
    @ExceptionHandler(Exception.class)
    // 一般只需要这三个参数
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage());
        // 遍历具体错误异常信息
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        // 固定的：要想看表现层是否是异步请求，就得按照这么个流程
        String xRequestedWith = request.getHeader("x-requested-with");
        // XMLHttpRequest是AJAX的核心
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJsonString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }

}
