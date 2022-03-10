package com.mycompany.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

@Configuration
public class AlphaConfig {

    @Bean
    public SimpleDateFormat simpleDateFormat() {
        //该方法返回的对象将被装配到bean里面
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

}
