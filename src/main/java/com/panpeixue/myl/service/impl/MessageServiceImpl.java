package com.panpeixue.myl.service.impl;

import com.panpeixue.myl.mapper.MessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.pojo.Message;
import com.panpeixue.myl.service.MessageService;
import com.panpeixue.myl.websocket.ChatWebSocketHandler;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final WebSocketSessionManager sessionManager;

    public MessageServiceImpl(MessageMapper messageMapper, UserMapper userMapper,
                              WebSocketSessionManager sessionManager) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    @CacheEvict(value = "messageList", allEntries = true)
    public Message send(Long senderId, Long receiverId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (userMapper.selectById(receiverId) == null) {
            throw new IllegalArgumentException("Receiver does not exist");
        }
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        messageMapper.insert(msg);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "New message", "message",
            "{\"messageId\":" + msg.getId() + ",\"senderId\":" + senderId + ",\"receiverId\":" + receiverId + "}"));
        return msg;
    }

    @Override
    @Cacheable(value = "messageList", key = "'received_' + #userId", sync = true)
    public List<Message> getReceived(Long userId) {
        return messageMapper.selectByReceiver(userId);
    }

    @Override
    @Cacheable(value = "messageList", key = "'sent_' + #userId", sync = true)
    public List<Message> getSent(Long userId) {
        return messageMapper.selectBySender(userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "messageList", allEntries = true)
    public void markRead(Long messageId, Long userId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) return;
        if (!msg.getReceiverId().equals(userId)) return;
        messageMapper.markRead(messageId);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "Message read", "read",
            "{\"messageId\":" + messageId + ",\"senderId\":" + msg.getSenderId() + "}"));
    }

    @Override
    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId);
    }
}