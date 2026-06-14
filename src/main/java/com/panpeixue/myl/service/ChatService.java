package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.model.pojo.ChatMessage;

import java.time.LocalDateTime;

public interface ChatService {
    ChatMessage saveChat(Long senderId, Long receiverId, String content);
    ChatHistoryResponse history(Long userId, LocalDateTime cursor, Integer size);
    Long findPartnerId(Long userId);
}
