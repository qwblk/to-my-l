package com.panpeixue.myl.service.impl;

import com.panpeixue.myl.mapper.DiaryMapper;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.service.DiaryService;
import com.panpeixue.myl.websocket.ChatWebSocketHandler;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiaryServiceImpl implements DiaryService {

    private final DiaryMapper diaryMapper;
    private final WebSocketSessionManager sessionManager;

    public DiaryServiceImpl(DiaryMapper diaryMapper, WebSocketSessionManager sessionManager) {
        this.diaryMapper = diaryMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    @CacheEvict(value = "diaryList", allEntries = true)
    public Diary create(Diary diary) {
        if (diary.getContent() == null || diary.getContent().isBlank()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        diaryMapper.insert(diary);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "New diary posted", "diary",
            "{\"diaryId\":" + diary.getId() + ",\"userId\":" + diary.getUserId() + "}"));
        return diary;
    }

    @Override
    @Cacheable(value = "diaryList", key = "'all'", sync = true)
    public List<Diary> listAll(Long currentUserId) {
        return diaryMapper.selectAll().stream()
            .filter(d -> d.getIsPrivate() == 0 || d.getUserId().equals(currentUserId))
            .collect(Collectors.toList());
    }

    @Override
    public List<Diary> listByUser(Long userId) {
        return diaryMapper.selectByUserId(userId);
    }
}