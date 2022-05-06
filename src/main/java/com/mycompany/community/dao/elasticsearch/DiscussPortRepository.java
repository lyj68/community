package com.mycompany.community.dao.elasticsearch;

import com.mycompany.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
// ElasticsearchRepository<DiscussPost,Integer> 第一个泛型指的是数据库实体类型，第二个指的是数据库实体类的主键
public interface DiscussPortRepository extends ElasticsearchRepository<DiscussPost,Integer> {
}
