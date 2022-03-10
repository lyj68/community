package com.mycompany.community.dao;

import com.mycompany.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    // offset行号，limit显示页面数目
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    //@Param用来给参数取别名；方法要动态查询调节参数，且这个条件只有一个，在<if>中使用，因此该参数之前必须要有别名
    int selectDiscussPostRows(@Param("userId") int userId);

}
