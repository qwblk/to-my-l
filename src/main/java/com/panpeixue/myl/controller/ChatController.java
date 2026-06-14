package com.panpeixue.myl.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.service.ChatService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/history")
    public Result<ChatHistoryResponse> history(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime cursor,
            @RequestParam(required = false) Integer size) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatService.history(userId, cursor, size));
    }
}
