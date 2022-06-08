package com.mycompany.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.mycompany.community.dao.DiscussPostMapper;
import com.mycompany.community.entity.DiscussPost;
import com.mycompany.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    public static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    //要能调用mapper，需要把mapper注入进来
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine核心接口: Cache, LoadingCache(一般我们使用这个), AsyncLoadingCache

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    // 缓存初始化在方法是调用前初始化一下
    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                //zui最大缓存数目
                .maximumSize(maxSize)
                // 缓存过期时间
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    // 该函数是在缓存读不到的时候，告诉他去哪查询加载数据
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存: Redis -> mysql

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        // 只有热帖显示才从缓存查询
        if (userId==0 && orderMode==1){
            // offset : limit相当于key
            return postListCache.get(offset + ":" + limit);
        }

        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //插入一个帖子，controller要给他discusspost对象
    public int addDiscussPost(DiscussPost post){
        if (post == null){
            throw new IllegalArgumentException("参数不能为空!");
        }

        //去除向html里面标签的符号
        //对其进行转义，以防止变成标签信息解析改变页面
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    //通过id去查询帖子
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    //更新帖子数目
    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    // 置顶
    public int updateType(int id, int type){
        return discussPostMapper.updateType(id,type);
    }

    // 加精
    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id,status);
    }


    // 更新分数
    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id,score);
    }
}
