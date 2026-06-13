package com.panpeixue.myl.service;

import com.panpeixue.myl.model.pojo.Diary;

import java.util.List;

public interface DiaryService {
    Diary create(Diary diary);
    List<Diary> listAll(Long currentUserId);
    List<Diary> listByUser(Long userId);
}