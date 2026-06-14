package com.panpeixue.myl.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the global handler maps NotLoginException to HTTP 401.
 * This is the path every protected endpoint (including /user/heartbeat
 * and /user/last-seen) takes when called without a valid sa-token —
 * SaServletFilter throws NotLoginException, this advice converts it.
 */
class GlobalExceptionHandlerNotLoginTest {

    @Test
    void notLoggedIn_returns401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NotLoginException ex = NotLoginException.newInstance(
                "login", NotLoginException.NOT_TOKEN, "no token", null);

        Result<?> result = handler.handleNotLogin(ex);

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMsg()).isNotBlank();
    }
}
