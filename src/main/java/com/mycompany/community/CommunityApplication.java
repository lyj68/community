package com.mycompany.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {

	// 管理bean的生存周期的：
	// 管理BEAN的初始化方法
	// 由该注解修饰的方法会在构造器执行完就被调用
	@PostConstruct
	public void init(){
		// 解决netty启动冲突问题
		// see Netty4Utils.setAvailbleProcessors()
		System.setProperty("es.set.netty.runtime.available.processors","false");
	}

	public static void main(String[] args) {
		//System.setProperty("es.set.netty.runtime.available.processors", "false");

		SpringApplication.run(CommunityApplication.class, args);
	}

}
