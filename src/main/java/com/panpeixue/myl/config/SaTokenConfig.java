package com.panpeixue.myl.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
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
                        "/user/login",
                        "/ws/**",
                        "/chat.html",
                        "/uploads/**",
                        "/upload",
                        "/", "/index.html"
                )
                .setAuth(obj -> SaRouter.match("/**", StpUtil::checkLogin))
                .setError(e -> SaResult.error(e.getMessage()));
    }
}