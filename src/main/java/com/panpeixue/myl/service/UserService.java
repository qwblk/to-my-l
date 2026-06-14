package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.UserLoginRequest;
import com.panpeixue.myl.model.pojo.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    List<User> listAll();

    Map<String, Object> login(UserLoginRequest dto);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    User updateInfo(Long userId, User update);

    /** 更新当前用户的 last_seen_at = NOW()。幂等。 */
    void heartbeat(Long userId);

    /** 读取当前用户的 last_seen_at（可能为 null）。 */
    LocalDateTime getLastSeenAt(Long userId);
}