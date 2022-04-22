package com.mycompany.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    // 生成随机字符串
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    //MD5加密
    // 只能加密不能解密
    // hello -> abc123def456
    // hello + 3e4a8 -> abc123def456abc
    public static String md5(String key){
        if (StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    //获取json字符串
    public static String getJsonString(int code , String msg, Map<String,Object> map){
        //获取json对象
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("code",code);
        jsonObject.put("msg",msg);

        if (map!=null){
            for (String key : map.keySet()){
                jsonObject.put(key,map.get(key));
            }
        }
        return jsonObject.toJSONString();

    }

    public static String getJsonString(int code , String msg){
        return getJsonString(code , msg, null);
    }

    public static String getJsonString(int code) {
        return getJsonString(code, null, null);
    }

    public static String getJsonString(int code , Map<String,Object> map){
        return getJsonString(code , null, map);
    }


    public static void main(String[] args) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name","lll");
        map.put("age",25);
        System.out.println(getJsonString(0,"ok",map));

    }

}
