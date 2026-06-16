package com.panpeixue.myl.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.DiaryCreateRequest;
import com.panpeixue.myl.model.dto.DiaryDayGroup;
import com.panpeixue.myl.model.dto.DiaryDaysResponse;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.service.DiaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @DeleteMapping("/{diaryId}")
    public Result<Void> delete(@PathVariable Long diaryId) {
        long userId = StpUtil.getLoginIdAsLong();
        diaryService.delete(diaryId, userId);
        return Result.ok();
    }

    @PutMapping("/{diaryId}/privacy")
    public Result<Diary> updatePrivacy(@PathVariable Long diaryId,
                                       @RequestBody java.util.Map<String, Integer> body) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(diaryService.updatePrivacy(diaryId, userId, body.get("isPrivate")));
    }

    @GetMapping("/days")
    public Result<DiaryDaysResponse> listDays(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate cursorDate,
            @RequestParam(required = false) Integer size) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(diaryService.listDays(userId, scope, cursorDate, size));
    }

    @GetMapping("/day")
    public Result<DiaryDayGroup> getDay(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "all") String scope) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(diaryService.getDay(userId, scope, date));
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