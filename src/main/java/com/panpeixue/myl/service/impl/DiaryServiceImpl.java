package com.panpeixue.myl.service.impl;

import com.panpeixue.myl.common.BizException;
import com.panpeixue.myl.mapper.DiaryMapper;
import com.panpeixue.myl.model.dto.DiaryDayGroup;
import com.panpeixue.myl.model.dto.DiaryDaysResponse;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.service.DiaryService;
import com.panpeixue.myl.websocket.ChatWebSocketHandler;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiaryServiceImpl implements DiaryService {

    private static final int DEFAULT_DAY_SIZE = 10;
    private static final int MAX_DAY_SIZE = 30;
    private static final String SCOPE_MINE = "mine";

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
    @Transactional
    @CacheEvict(value = "diaryList", allEntries = true)
    public void delete(Long diaryId, Long userId) {
        Diary diary = diaryMapper.selectById(diaryId);
        if (diary == null || diary.getDeletedAt() != null) {
            throw BizException.notFound("Diary not found");
        }
        if (!diary.getUserId().equals(userId)) {
            throw BizException.forbidden("No permission");
        }

        diaryMapper.softDelete(diaryId);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "Diary deleted", "diary_delete",
            "{\"diaryId\":" + diaryId + ",\"userId\":" + userId + "}"));
    }

    @Override
    @Cacheable(value = "diaryList", key = "#currentUserId", sync = true)
    public List<Diary> listAll(Long currentUserId) {
        return diaryMapper.selectAll().stream()
            .filter(d -> d.getIsPrivate() == 0 || d.getUserId().equals(currentUserId))
            .collect(Collectors.toList());
    }

    @Override
    public List<Diary> listByUser(Long userId) {
        return diaryMapper.selectByUserId(userId);
    }

    @Override
    public DiaryDaysResponse listDays(Long currentUserId, String scope, LocalDate cursorDate, Integer size) {
        int pageSize = clampSize(size, DEFAULT_DAY_SIZE, MAX_DAY_SIZE);
        boolean mine = isMineScope(scope);

        // 多查 1 天用于 hasMore，返回时只取前 pageSize 天。
        List<LocalDate> dates = mine
            ? diaryMapper.selectVisibleDatesMine(currentUserId, cursorDate, pageSize + 1)
            : diaryMapper.selectVisibleDatesAll(currentUserId, cursorDate, pageSize + 1);

        boolean hasMore = dates.size() > pageSize;
        List<LocalDate> pageDates = hasMore ? dates.subList(0, pageSize) : dates;
        List<DiaryDayGroup> groups = new ArrayList<>(pageDates.size());
        for (LocalDate date : pageDates) {
            groups.add(buildDayGroup(currentUserId, mine, date));
        }

        String nextCursorDate = groups.isEmpty() ? null : groups.get(groups.size() - 1).getDate();
        return new DiaryDaysResponse(groups, nextCursorDate, hasMore);
    }

    @Override
    public DiaryDayGroup getDay(Long currentUserId, String scope, LocalDate date) {
        return buildDayGroup(currentUserId, isMineScope(scope), date);
    }

    // ==================== helpers ====================

    private DiaryDayGroup buildDayGroup(Long currentUserId, boolean mine, LocalDate date) {
        List<Diary> entries = mine
            ? diaryMapper.selectVisibleEntriesByDateMine(currentUserId, date)
            : diaryMapper.selectVisibleEntriesByDateAll(currentUserId, date);
        return new DiaryDayGroup(date.toString(), weekdayCn(date), entries);
    }

    private boolean isMineScope(String scope) {
        return SCOPE_MINE.equalsIgnoreCase(scope);
    }

    private int clampSize(Integer size, int defaultSize, int maxSize) {
        if (size == null) return defaultSize;
        if (size < 1) return defaultSize;
        return Math.min(size, maxSize);
    }

    private String weekdayCn(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }
}
