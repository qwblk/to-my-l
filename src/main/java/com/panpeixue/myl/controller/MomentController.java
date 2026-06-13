package com.panpeixue.myl.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.MomentCommentRequest;
import com.panpeixue.myl.model.dto.MomentCreateRequest;
import com.panpeixue.myl.model.pojo.Moment;
import com.panpeixue.myl.model.pojo.MomentComment;
import com.panpeixue.myl.model.pojo.MomentLike;
import com.panpeixue.myl.service.MomentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/moment")
public class MomentController {

    private final MomentService momentService;

    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    @PostMapping
    public Result<Moment> create(@RequestBody MomentCreateRequest req) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(momentService.create(userId, req.getContent(), req.getImage()));
    }

    @GetMapping("/all")
    public Result<List<Moment>> listAll() {
        return Result.ok(momentService.listAll());
    }

    @PostMapping("/like/{momentId}")
    public Result<Boolean> toggleLike(@PathVariable Long momentId) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(momentService.toggleLike(momentId, userId));
    }

    @GetMapping("/like/{momentId}")
    public Result<List<MomentLike>> getLikes(@PathVariable Long momentId) {
        return Result.ok(momentService.getLikes(momentId));
    }

    @PostMapping("/comment/{momentId}")
    public Result<MomentComment> comment(@PathVariable Long momentId,
                                         @RequestBody MomentCommentRequest req) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(momentService.addComment(momentId, userId, req.getContent()));
    }

    @GetMapping("/comment/{momentId}")
    public Result<List<MomentComment>> getComments(@PathVariable Long momentId) {
        return Result.ok(momentService.getComments(momentId));
    }
}