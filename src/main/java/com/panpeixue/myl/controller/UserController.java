package com.panpeixue.myl.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.UserLoginRequest;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.UserService;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final DateTimeFormatter LAST_SEEN_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final WebSocketSessionManager sessionManager;

    public UserController(UserService userService, WebSocketSessionManager sessionManager) {
        this.userService = userService;
        this.sessionManager = sessionManager;
    }

    @SaIgnore
    @PostMapping("/login")
    public SaResult login(@RequestBody UserLoginRequest dto) {
        Map<String, Object> result = userService.login(dto);
        return SaResult.ok("Login success").setData(result);
    }

    @GetMapping("/me")
    public Result<User> me() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.getById(userId));
    }

    @PutMapping("/me")
    public Result<User> updateMe(@RequestBody User update) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateInfo(userId, update));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<User>> listAll() {
        return Result.ok(userService.listAll());
    }

    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    @PutMapping("/info")
    public Result<User> updateInfo(@RequestBody User update) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateInfo(userId, update));
    }

    @GetMapping("/online-status")
    public Result<Set<String>> onlineStatus() {
        return Result.ok(sessionManager.onlineUsers());
    }

    /**
     * 心跳：更新当前用户 last_seen_at = NOW()。
     * 前端每 30s（visibilityState=visible 时）+ beforeunload 各调一次。
     * 幂等，连调多次只更新时间戳。
     */
    @PutMapping("/heartbeat")
    public Result<Void> heartbeat() {
        long userId = StpUtil.getLoginIdAsLong();
        userService.heartbeat(userId);
        return Result.ok();
    }

    /**
     * 取上次活跃时间。前端登录后先调这个拿到「上次离开时间」，
     * 再调 /user/heartbeat 把它推到 NOW()，用作离线追赶的起点。
     * 登录接口本身不会更新 last_seen_at，所以这里返回的是真正的上次离开时间。
     */
    @GetMapping("/last-seen")
    public Result<Map<String, String>> lastSeen() {
        long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime ts = userService.getLastSeenAt(userId);
        String formatted = ts == null ? null : ts.format(LAST_SEEN_FMT);
        return Result.ok(Collections.singletonMap("lastSeenAt", formatted));
    }

    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("Logged out");
    }
}