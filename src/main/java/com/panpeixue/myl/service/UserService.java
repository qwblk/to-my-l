package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.UserLoginRequest;
import com.panpeixue.myl.model.pojo.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    List<User> listAll();

    Map<String, Object> login(UserLoginRequest dto);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    User updateInfo(Long userId, User update);
}