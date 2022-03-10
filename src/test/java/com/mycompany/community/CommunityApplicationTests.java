package com.mycompany.community;

import com.mycompany.community.dao.AlphaDao;
import com.mycompany.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
//要想使用这个容器，必须这个类取实现一个ApplicationContextAware接口
class CommunityApplicationTests implements ApplicationContextAware {
	//记录一下这个容器
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Test
	public void testApplicationContext() {

		System.out.println(applicationContext);
		//按照类型获取Bean
		AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
		System.out.println(alphaDao.select());

		alphaDao = applicationContext.getBean("alphaHiternate", AlphaDao.class);
		System.out.println(alphaDao.select());


	}

	@Test
	public void testBeanManagement() {
		AlphaService alphaService = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);

		alphaService = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);
	}

	@Test
	public void testBeanConfig() {
		SimpleDateFormat simpleDateFormat = applicationContext.getBean(SimpleDateFormat.class);
		System.out.println(simpleDateFormat.format(new Date()));
	}

//	//自动获取，自动注入
//	@Autowired
//	//希望spring将Alphadao注入给alphaDao
//	private AlphaDao alphaDao;

	//如果希望spring将获得alphaHibernate
	//该注解可以加在类的构造器或者加在set方法之前，但我们一般是加在属性之前
	@Autowired
	@Qualifier("alphaHiternate")
	private AlphaDao alphaDao;

	@Autowired
	private  AlphaService alphaService;

	@Autowired
	private  SimpleDateFormat simpleDateFormat;

	@Test
	public void testDI(){
		System.out.println(alphaDao);
		System.out.println(alphaService);
		System.out.println(simpleDateFormat);
	}


//	@Test
//	void contextLoads() {
//	}



}
