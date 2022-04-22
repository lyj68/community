package com.mycompany.community.service;

import com.mycompany.community.dao.MessageMapper;
import com.mycompany.community.entity.Message;
import com.mycompany.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    // 查询会话
    public List<Message> findConversations(int userId , int offset, int limit){
        return messageMapper.selectConversations(userId,offset,limit);
    }

    // 查询会话数
    public int findConversationCount(int userId){
        return messageMapper.selectConversationCount(userId);
    }

    // 查询当前消息详情列表
    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message){
        // 过滤标签
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        // 过滤敏感词
        message.setContent(sensitiveFilter.filter(message.getContent()));

        return messageMapper.insertMessage(message);

    }

    public int readMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids,1);
    }
}
