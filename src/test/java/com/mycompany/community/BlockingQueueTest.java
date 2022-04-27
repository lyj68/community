package com.mycompany.community;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTest {

    public static void main(String[] args) {
        // 创建一个公共的阻塞队列
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(10);
        // 创建生成者线程
        new Thread(new Producer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
    }

}

// 生产者和消费者要共用一个队列
// 生产者和消费者都是线程，需要实现runnable接口
class Producer implements Runnable{

    // 重载一个构造器，用于公用队列
    private BlockingQueue<Integer> queue ;

    public Producer(BlockingQueue queue){
        this.queue = queue;
    }


    @Override
    public void run() {
        try {
            // 生产者最多生产100个数据
            for (int i=0;i<100;i++){
                Thread.sleep(20);
                queue.put(i);
                System.out.println(Thread.currentThread().getName() + "生产:" + queue.size());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

class Consumer implements Runnable{

    // 重载一个构造器，用于公用队列
    private BlockingQueue<Integer> queue ;

    public Consumer(BlockingQueue queue){
        this.queue = queue;
    }


    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(new Random().nextInt(1000));
                // 消费者消费
                queue.take();
                System.out.println(Thread.currentThread().getName() + "消费:" + queue.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
