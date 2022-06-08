package com.mycompany.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;


@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class WkTests {

    @Test
    public void wkTest() {
        String cmd = "D:/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com D:/java_program/data/wk-image/4.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
