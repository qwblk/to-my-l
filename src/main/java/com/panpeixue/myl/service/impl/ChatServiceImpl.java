package com.panpeixue.myl.service.impl;

import com.panpeixue.myl.common.BizException;
import com.panpeixue.myl.mapper.ChatMessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.model.pojo.ChatMessage;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.ChatService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_SIZE = 30;
    private static final int MAX_SIZE = 50;
    public static final int MAX_CONTENT_LENGTH = 500;
    private static final DateTimeFormatter CURSOR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper, UserMapper userMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public ChatMessage saveChat(Long senderId, Long receiverId, String content) {
        String text = normalizeContent(content);
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(text);
        msg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);
        return msg;
    }

    @Override
    public ChatHistoryResponse history(Long userId, LocalDateTime cursor, Integer size) {
        int pageSize = clampSize(size);
        Long partnerId = findPartnerId(userId);
        List<ChatMessage> rows = chatMessageMapper.selectHistoryPage(userId, partnerId, cursor, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<ChatMessage> list = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = list.isEmpty() || list.get(list.size() - 1).getCreateTime() == null
            ? null
            : list.get(list.size() - 1).getCreateTime().format(CURSOR_FMT);
        return new ChatHistoryResponse(list, nextCursor, hasMore);
    }

    @Override
    public Long findPartnerId(Long userId) {
        return userMapper.selectAll().stream()
            .map(User::getId)
            .filter(id -> !id.equals(userId))
            .findFirst()
            .orElseThrow(() -> BizException.notFound("Partner not found"));
    }

    private String normalizeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat content cannot be empty");
        }
        String text = content.trim();
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Chat content length cannot exceed " + MAX_CONTENT_LENGTH);
        }
        return text;
    }

    private int clampSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}
