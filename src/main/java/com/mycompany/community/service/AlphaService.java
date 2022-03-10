package com.mycompany.community.service;

import com.mycompany.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//希望该bean能由容器初始化和销毁
@Service

//如果想每次访问都返回一个新的实例加注解
//@Scope("prototype")
public class AlphaService {


    //注入dao
    @Autowired
    private  AlphaDao alphaDao;

    public AlphaService() {
        System.out.println("实例化AlphaService");
    }

    //要想让容器帮忙管理这个方法
    //这个方法会在构造器（构造函数）之后调用
    @PostConstruct
    public void init() {
        System.out.println("初始化AlphaService");
    }

    @PreDestroy
    //在对象销毁之前调用他
    public void destory() {
        System.out.println("销毁AlphaService");
    }

    public String find(){
        return alphaDao.select();
    }

}
