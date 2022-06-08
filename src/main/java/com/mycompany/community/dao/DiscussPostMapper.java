package com.mycompany.community.dao;

import com.mycompany.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    // offset行号，limit显示页面数目
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    //@Param用来给参数取别名；方法要动态查询调节参数，且这个条件只有一个，在<if>中使用，因此该参数之前必须要有别名
    int selectDiscussPostRows(@Param("userId") int userId);

    //dao层申明一个方法插入
    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    // 修改帖子评论的数量
    int updateCommentCount(int id, int commentCount);

    // 置顶
    int updateType(int id, int type);

    // 加精
    int updateStatus(int id, int status);

    // 更新分数
    int updateScore(int id, double score);

}
