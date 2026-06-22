package com.panpeixue.myl.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenConfig {

    @Bean
    public SaServletFilter saServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude(
                        "/favicon.ico",
                        "/health",
                        "/api/health",
                        "/user/login",
                        "/analytics/event",
                        "/analytics/summary",
                        "/analytics/recent",
                        "/ws/**",
                        "/chat.html",
                        "/uploads/**",
                        "/static/**",
                        "/", "/index.html"
                )
                .setAuth(obj -> {
                    /* skip auth for OPTIONS preflight */
                    if (!"OPTIONS".equals(SaHolder.getRequest().getMethod())) {
                        SaRouter.match("/**", StpUtil::checkLogin);
                    }
                })
                .setError(e -> {
                    /*
                     * SaServletFilter 抛出来的异常是 servlet 层直接处理的，
                     * 不会经过 @RestControllerAdvice，所以这里需要复刻 GlobalExceptionHandler
                     * 同样的格式：401 / 403 / 500，使用项目统一的 Result 结构。
                     * 不要用 SaResult.error，它有自己的 code/msg 约定，会和前端拦截器对不上。
                     */
                    SaHolder.getResponse()
                        .setHeader("Content-Type", "application/json;charset=utf-8")
                        .setHeader("Access-Control-Allow-Origin", "*")
                        .setHeader("Access-Control-Allow-Methods", "*")
                        .setHeader("Access-Control-Allow-Headers", "*");
                    if (e instanceof NotLoginException nle) {
                        // 被同账号新登录顶下线 / 被管理员踢下线 → 前端约定的 4012；
                        // 其它原因（无 token / 过期 / 无效）→ 401。
                        if (NotLoginException.BE_REPLACED.equals(nle.getType())
                                || NotLoginException.KICK_OUT.equals(nle.getType())) {
                            return json(4012, "Logged in elsewhere");
                        }
                        return json(401, "Please login first");
                    }
                    if (e instanceof NotPermissionException || e instanceof NotRoleException) {
                        return json(403, "No permission");
                    }
                    return json(500, e.getMessage());
                });
    }

    private String json(int code, String msg) {
        return "{\"code\":" + code
                + ",\"msg\":\"" + escape(msg)
                + "\",\"data\":null}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}