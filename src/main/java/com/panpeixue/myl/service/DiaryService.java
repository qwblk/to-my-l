package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.DiaryDayGroup;
import com.panpeixue.myl.model.dto.DiaryDaysResponse;
import com.panpeixue.myl.model.pojo.Diary;

import java.time.LocalDate;
import java.util.List;

public interface DiaryService {
    Diary create(Diary diary);
    void delete(Long diaryId, Long userId);
    List<Diary> listAll(Long currentUserId);
    List<Diary> listByUser(Long userId);
    DiaryDaysResponse listDays(Long currentUserId, String scope, LocalDate cursorDate, Integer size);
    DiaryDayGroup getDay(Long currentUserId, String scope, LocalDate date);
}