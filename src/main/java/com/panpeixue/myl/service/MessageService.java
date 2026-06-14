package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.MessagePageResponse;
import com.panpeixue.myl.model.pojo.Message;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {
    Message send(Long senderId, Long receiverId, String content);
    List<Message> getReceived(Long userId);
    List<Message> getSent(Long userId);
    MessagePageResponse getReceivedPage(Long userId, LocalDateTime cursor, Integer size);
    MessagePageResponse getSentPage(Long userId, LocalDateTime cursor, Integer size);
    void markRead(Long messageId, Long userId);
    int countUnread(Long userId);
}