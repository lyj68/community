package com.mycompany.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CommunityApplication.class)
public class kafkaTest {

    @Autowired
    private kafkaProducer kafkaProducer;

    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test","nihao");
        kafkaProducer.sendMessage("test","aaa");

        try {
            Thread.sleep(1000*8);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

@Component
class kafkaProducer{

    @Autowired
    public KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content){
        kafkaTemplate.send(topic,content);
    }


}

@Component
class kafkaConsumer{

    // 消费者被动监听，然后如果获得消息就由以下方法自己去处理消息
    @KafkaListener(topics = {"test"})
    public void handleMessage(ConsumerRecord record){
        System.out.println(record.value());
    }

}
