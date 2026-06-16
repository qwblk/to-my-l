package com.panpeixue.myl.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== Sa-Token ====================

    /**
     * sa-token 抛 NotLoginException 时按 type 分流：
     *   BE_REPLACED / KICK_OUT —— 被顶下线 / 被强制踢下线，返回 4012 让前端弹专门提示
     *   其它（NOT_TOKEN / INVALID_TOKEN / TOKEN_TIMEOUT / TOKEN_FREEZE 等）—— 401，前端走"未登录"分支
     *
     * 4012 这条对前端的契约是「曾经登录但被挤掉」，前端要展示"已在其他设备登录"等提示再跳登录页。
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        if (NotLoginException.BE_REPLACED.equals(e.getType())
                || NotLoginException.KICK_OUT.equals(e.getType())) {
            log.warn("Logged in elsewhere: {}", e.getMessage());
            return Result.error(4012, "Logged in elsewhere");
        }
        log.warn("Not login: {}", e.getMessage());
        return Result.error(401, "Please login first");
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<?> handleNotPermission(Exception e) {
        log.warn("No permission: {}", e.getMessage());
        return Result.error(403, "No permission");
    }

    // ==================== Request parsing ====================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed JSON: {}", e.getMessage());
        return Result.error(400, "Request body is malformed or missing");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing param: {}", e.getParameterName());
        return Result.error(400, "Missing required parameter: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {} expected {}", e.getName(),
                Objects.requireNonNullElse(e.getRequiredType(), Object.class).getSimpleName());
        return Result.error(400, "Invalid value for: " + e.getName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMethod());
        return Result.error(405, "Method " + e.getMethod() + " not supported");
    }

    // ==================== Business ====================

    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        log.warn("Biz error {}: {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad argument: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    // ==================== Database ====================

    @ExceptionHandler(DataAccessException.class)
    public Result<?> handleDataAccess(DataAccessException e) {
        log.error("Database error", e);
        return Result.error(500, "Database error");
    }

    // ==================== Fallback ====================

    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointer(NullPointerException e) {
        log.error("NullPointer", e);
        return Result.error(500, "Internal server error");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("System error", e);
        return Result.error(500, "Server busy, try later");
    }
}