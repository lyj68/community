package com.mycompany.community;

import com.mycompany.community.dao.DiscussPostMapper;
import com.mycompany.community.dao.elasticsearch.DiscussPortRepository;
import com.mycompany.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPortRepository discussPortRepository;

    @Autowired
    private DiscussPostMapper discussPostMapper;


    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    //@Test
    public void testInsert(){
        // ?????????????????????????????????????????????
        // ????????????????????????
        discussPortRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPortRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPortRepository.save(discussPostMapper.selectDiscussPostById(243));
        discussPortRepository.save(discussPostMapper.selectDiscussPostById(244));
        discussPortRepository.save(discussPostMapper.selectDiscussPostById(245));
    }

    //@Test
    public void testInsertList() {
        // ???????????????????????????elasticsearch
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(101,0,100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(102, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(111, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(112, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(131, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(132, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(133, 0, 100,0));
        discussPortRepository.saveAll(discussPostMapper.selectDiscussPosts(134, 0, 100,0));
    }

    //@Test
    public void testUpdate() {
        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
        post.setContent("????????????,????????????.");
        discussPortRepository.save(post);
    }

    //@Test
    public void testDelete() {
        // discussRepository.deleteById(231);
        discussPortRepository.deleteAll();
    }

    //@Test
    public void test1() {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
//????????????
                .withQuery(QueryBuilders.queryStringQuery("???????????????").defaultField("title"))
//??????
                .withPageable(PageRequest.of(0, 5))
//??????
                .withSort(SortBuilders.fieldSort("id").order(SortOrder.DESC))
//??????????????????
                .withHighlightFields(new HighlightBuilder.Field("??????"))
                .build();
        List<DiscussPost> articleEntities = elasticsearchTemplate.queryForList(nativeSearchQuery, DiscussPost.class);
        articleEntities.forEach(item -> System.out.println(item.toString()));
    }



    //@Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // ????????????????????????????????????????????????
                .withQuery(QueryBuilders.multiMatchQuery("???????????????", "title", "content"))
                //?????????????????????????????????
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // ?????????????????????????????????????????????????????????
                .withPageable(PageRequest.of(0, 10))
                // ??????????????? ??????????????????
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        System.out.println(searchQuery);

        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // ???????????????????????????????????????, ??????????????????.

        Page<DiscussPost> page = discussPortRepository.search(searchQuery);
        // ???????????????????????????
        System.out.println(page.getTotalElements());
        // ??????????????????
        System.out.println(page.getTotalPages());
        // ???????????????
        System.out.println(page.getNumber());
        // ????????????????????????
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }


    }

    //@Test
    public void testSearchByTemplate() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("??????", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                // ????????????
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }
                // ????????????????????????json????????????????????????????????????
                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // ???????????????????????????
                    // ???????????????????????????????????????
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        // ?????????????????????????????????
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }





}
