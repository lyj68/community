package com.mycompany.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {

    public static String getCookie(HttpServletRequest request,String name){

        if (request==null || name==null){
            throw new IllegalArgumentException("参数为空!");
        }

        //从request中获取cookie数组
        Cookie[] cookies = request.getCookies();
        //遍历数组查询所需要的cookie
        if (cookies!=null){
            for (Cookie cookie: cookies) {
                if(cookie.getName().equals(name)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
