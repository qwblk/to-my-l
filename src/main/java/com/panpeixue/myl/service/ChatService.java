package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatService {
    ChatMessage saveChat(Long senderId, Long receiverId, String content, List<MomentMedia> mediaList);
    ChatHistoryResponse history(Long userId, LocalDateTime cursor, Integer size);
    ChatHistoryResponse history(Long userId, LocalDateTime cursor, Long cursorId, Integer size);
    Long findPartnerId(Long userId);
}
