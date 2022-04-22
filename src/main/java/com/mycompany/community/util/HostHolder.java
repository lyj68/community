package com.mycompany.community.util;

import com.mycompany.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替session对象
 */

@Component
public class HostHolder {

    //利用线程池，来完成隔离性，以防止多线程带来的问题
    private ThreadLocal<User> users = new ThreadLocal<>();
    //以当前   线程为key存取值的；主要是使用get和set方法

    //给他从ticket找到的user,把他存入线程池
    public void setUser(User user) {
        users.set(user);
    }

    //从线程池获取user
    public User getUser() {
        return users.get();
    }

    //清除信息
    public void clear(){
        users.remove();
    }

}
