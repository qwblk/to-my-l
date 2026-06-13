package com.panpeixue.myl.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.DiaryCreateRequest;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.service.DiaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping
    public Result<Diary> create(@RequestBody DiaryCreateRequest req) {
        long userId = StpUtil.getLoginIdAsLong();
        Diary diary = new Diary();
        diary.setUserId(userId);
        diary.setTitle(req.getTitle());
        diary.setContent(req.getContent());
        diary.setMood(req.getMood());
        diary.setWeather(req.getWeather());
        diary.setIsPrivate(req.getIsPrivate());
        return Result.ok(diaryService.create(diary));
    }

    @GetMapping("/all")
    public Result<List<Diary>> listAll() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(diaryService.listAll(userId));
    }

    @GetMapping("/mine")
    public Result<List<Diary>> listMine() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(diaryService.listByUser(userId));
    }
}