package com.panpeixue.myl.service.impl;

import com.panpeixue.myl.mapper.MessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.MessagePageResponse;
import com.panpeixue.myl.model.pojo.Message;
import com.panpeixue.myl.service.MessageService;
import com.panpeixue.myl.websocket.ChatWebSocketHandler;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter CURSOR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    public MessagePageResponse getReceivedPage(Long userId, LocalDateTime cursor, Integer size) {
        return getReceivedPage(userId, cursor, null, size);
    }

    @Override
    public MessagePageResponse getReceivedPage(Long userId, LocalDateTime cursor, Long cursorId, Integer size) {
        int pageSize = clampSize(size);
        List<Message> rows = messageMapper.selectReceivedPage(userId, cursor, cursorId, pageSize + 1);
        return toPage(rows, pageSize);
    }

    @Override
    public MessagePageResponse getSentPage(Long userId, LocalDateTime cursor, Integer size) {
        return getSentPage(userId, cursor, null, size);
    }

    @Override
    public MessagePageResponse getSentPage(Long userId, LocalDateTime cursor, Long cursorId, Integer size) {
        int pageSize = clampSize(size);
        List<Message> rows = messageMapper.selectSentPage(userId, cursor, cursorId, pageSize + 1);
        return toPage(rows, pageSize);
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

    private MessagePageResponse toPage(List<Message> rows, int pageSize) {
        boolean hasMore = rows.size() > pageSize;
        List<Message> list = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = list.isEmpty() || list.get(list.size() - 1).getCreateTime() == null
            ? null
            : list.get(list.size() - 1).getCreateTime().format(CURSOR_FMT);
        return new MessagePageResponse(list, nextCursor, hasMore);
    }

    private int clampSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
