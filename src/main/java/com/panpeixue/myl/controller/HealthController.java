package com.panpeixue.myl.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 给前端 nginx HEALTHCHECK 探活用的端点。
 *
 * 不查 DB、不查 Redis、不触发任何下游依赖——只证 Spring 进程能接 HTTP 请求即可。
 * 数据库/缓存的活性由 docker-compose 的 healthcheck 各自负责，不要在这里聚合，
 * 否则一个下游抖动会让整个前端探活红掉。
 *
 * 路径：直连后端是 /health；经前端 nginx 反代时是 /api/health（nginx 剥 /api 前缀）。
 */
@RestController
public class HealthController {

    @SaIgnore
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
