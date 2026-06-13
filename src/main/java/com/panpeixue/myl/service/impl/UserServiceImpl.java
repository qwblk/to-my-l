package com.panpeixue.myl.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.UserLoginRequest;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Cacheable(value = "userCache", key = "#id", sync = true)
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public List<User> listAll() {
        return userMapper.selectAll();
    }

    @Override
    public Map<String, Object> login(UserLoginRequest dto) {
        User user = userMapper.selectWithPassword(dto.getUsername());
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        log.info("User {} logged in", dto.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenInfo.getTokenValue());

        /* first login greeting */
        if (user.getIsFirstLogin() != null && user.getIsFirstLogin() == 1) {
            userMapper.clearFirstLogin(user.getId());
            result.put("firstLogin", true);
            result.put("greeting", "Welcome to our little space, " + user.getName()
                + "! This is a private place just for the two of us. "
                + "You can write diaries, share moments, leave messages, and chat. Enjoy!");
        } else {
            result.put("firstLogin", false);
        }
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "userCache", key = "#userId")
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectWithPassword(userMapper.selectById(userId).getUsername());
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        userMapper.updatePassword(userId, encoder.encode(newPassword));
        log.info("User {} changed password", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userCache", key = "#userId")
    public User updateInfo(Long userId, User update) {
        if (update.getName() == null || update.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        update.setId(userId);
        userMapper.updateInfo(update);
        log.info("User {} updated info", userId);
        return userMapper.selectById(userId);
    }
}