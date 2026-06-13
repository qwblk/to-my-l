package com.panpeixue.myl.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.MessageSendRequest;
import com.panpeixue.myl.model.pojo.Message;
import com.panpeixue.myl.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public Result<Message> send(@RequestBody MessageSendRequest req) {
        long senderId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.send(senderId, req.getReceiverId(), req.getContent()));
    }

    @GetMapping("/received")
    public Result<List<Message>> received() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getReceived(userId));
    }

    @GetMapping("/sent")
    public Result<List<Message>> sent() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getSent(userId));
    }

    @PutMapping("/read/{messageId}")
    public Result<?> markRead(@PathVariable Long messageId) {
        long userId = StpUtil.getLoginIdAsLong();
        messageService.markRead(messageId, userId);
        return Result.ok();
    }

    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.countUnread(userId));
    }

    @GetMapping("/coming-soon")
    public Result<String> comingSoon() {
        return Result.ok("Coming soon...");
    }
}