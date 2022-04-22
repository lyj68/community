package com.mycompany.community;


import com.mycompany.community.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CommunityApplication.class)
public class SenstiveTests {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSentive(){
        String text = "可以赌博，可以吸毒，哈哈哈";
        String s = sensitiveFilter.filter(text);
        System.out.println(s);
    }

}
