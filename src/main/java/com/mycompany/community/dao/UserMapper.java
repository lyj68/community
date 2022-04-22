package com.mycompany.community.dao;

import com.mycompany.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

//操作数据表user的mapper接口

@Mapper
public interface UserMapper {

    // 根据id查user
    User selectById(int id);
    //根据用户名查user
    User selectByName(String username);
    //根据邮箱查user
    User selectByEmail(String email);

    //插入用户
    int insertUser(User user);

    //修改用户状态
    int updateStatus(int id,int status);

    //更新头像
    int updateHeader(int id,String headerUrl);

    //更新密码
    int updatePassword(int id, String password);

}
