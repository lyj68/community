package com.mycompany.community.service;

import com.mycompany.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId, int entityUserId){
//        // 获取key
//        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
//        // 先判断当前用户是否点过赞
//        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
//
//        // 1点赞，2取消
//        if (isMember){
//            redisTemplate.opsForSet().remove(key,userId);
//        }else {
//            redisTemplate.opsForSet().add(key,userId);
//        }
        redisTemplate.execute(new SessionCallback(){
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    // 获取当前key
                    String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                    String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                    // 判断是否点了赞
                    Boolean ismember = operations.opsForSet().isMember(entityLikeKey, userId);

                    // 事务开始
                    operations.multi();

                    // 对redis进行更改
                    // 1次点赞且用户赞-1，2次取消且用户赞+1
                    if (ismember){
                        operations.opsForSet().remove(entityLikeKey,userId);
                        operations.opsForValue().decrement(userLikeKey);
                    }else {
                        operations.opsForSet().add(entityLikeKey,userId);
                        operations.opsForValue().increment(userLikeKey);
                    }

                    //事务提交
                    return operations.exec();
                }
            }
        );
    }

    // 点赞数目
    public long findEntityLikeCount(int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(key);
    }

    // 查询点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
        return isMember? 1: 0;
    }

    // 查询某个用户获得的赞的数目
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }
}
