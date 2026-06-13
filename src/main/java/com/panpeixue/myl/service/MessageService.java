package com.panpeixue.myl.service;

import com.panpeixue.myl.model.pojo.Message;

import java.util.List;

public interface MessageService {
    Message send(Long senderId, Long receiverId, String content);
    List<Message> getReceived(Long userId);
    List<Message> getSent(Long userId);
    void markRead(Long messageId, Long userId);
    int countUnread(Long userId);
}