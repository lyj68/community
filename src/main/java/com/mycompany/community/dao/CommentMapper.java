package com.mycompany.community.dao;

import com.mycompany.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    //根据帖子查询评论集合
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    // 根据帖子键值查询评论数目
    int selectCountByEntity(int entityType, int entityId);

    // 增加评论操作
    int insertComment(Comment comment);

    // 查询评论的用户
    Comment findCommentById(int id);

}
