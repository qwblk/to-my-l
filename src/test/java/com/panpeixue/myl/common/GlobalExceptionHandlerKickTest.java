package com.panpeixue.myl.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 「未登录」与「被顶下线 / 被踢下线」走同一个 NotLoginException，但 type 不同：
 *   NOT_TOKEN / TOKEN_TIMEOUT / INVALID_TOKEN / TOKEN_FREEZE / NO_TOKEN  → 401
 *   BE_REPLACED（同账号新登录顶掉旧 token）/ KICK_OUT（管理员强制踢下线） → 4012
 *
 * 4012 这条契约让前端可以区分「该跳登录页」vs「弹‘已在其他设备登录’再跳登录页」。
 */
class GlobalExceptionHandlerKickTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notLoggedIn_returns401() {
        NotLoginException ex = NotLoginException.newInstance(
                "login", NotLoginException.NOT_TOKEN, "no token", null);

        Result<?> result = handler.handleNotLogin(ex);

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMsg()).isEqualTo("Please login first");
    }

    @Test
    void beReplaced_returns4012() {
        NotLoginException ex = NotLoginException.newInstance(
                "login", NotLoginException.BE_REPLACED, "kicked by new login", null);

        Result<?> result = handler.handleNotLogin(ex);

        assertThat(result.getCode()).isEqualTo(4012);
        assertThat(result.getMsg()).isEqualTo("Logged in elsewhere");
    }

    @Test
    void kickOut_returns4012() {
        NotLoginException ex = NotLoginException.newInstance(
                "login", NotLoginException.KICK_OUT, "force kicked", null);

        Result<?> result = handler.handleNotLogin(ex);

        assertThat(result.getCode()).isEqualTo(4012);
        assertThat(result.getMsg()).isEqualTo("Logged in elsewhere");
    }

    @Test
    void invalidToken_returns401_notKicked() {
        NotLoginException ex = NotLoginException.newInstance(
                "login", NotLoginException.INVALID_TOKEN, "bad token", null);

        Result<?> result = handler.handleNotLogin(ex);

        assertThat(result.getCode()).isEqualTo(401);
    }

    @Test
    void tokenTimeout_returns401_notKicked() {
        NotLoginException ex = NotLoginException.newInstance(
                "login", NotLoginException.TOKEN_TIMEOUT, "expired", null);

        Result<?> result = handler.handleNotLogin(ex);

        assertThat(result.getCode()).isEqualTo(401);
    }
}
