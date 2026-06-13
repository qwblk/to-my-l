package com.panpeixue.myl.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.UserLoginRequest;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<User>> listAll() {
        return Result.ok(userService.listAll());
    }

    /** Change password */
    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /** Update name / gender / birthday / bio */
    @PutMapping("/info")
    public Result<User> updateInfo(@RequestBody User update) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateInfo(userId, update));
    }

    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("Logged out");
    }
}