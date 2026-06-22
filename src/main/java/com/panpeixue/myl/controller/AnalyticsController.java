package com.panpeixue.myl.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.panpeixue.myl.common.Result;
import com.panpeixue.myl.model.dto.AnalyticsEventRequest;
import com.panpeixue.myl.model.dto.AnalyticsSummaryResponse;
import com.panpeixue.myl.model.pojo.AnalyticsEvent;
import com.panpeixue.myl.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * 匿名访问也允许记录；如果 Authorization token 有效，则顺手关联 userId。
     *
     * 注意：本项目有 SaServletFilter，全局鉴权发生在 controller 之前，@SaIgnore 只是语义说明；
     * 真正放行要同步维护 SaTokenConfig.addExclude("/analytics/event")。
     */
    @SaIgnore
    @PostMapping("/event")
    public Result<Void> event(@RequestBody AnalyticsEventRequest req, HttpServletRequest request) {
        Long userId = optionalUserId(request.getHeader("Authorization"));
        analyticsService.record(req, userId, clientIp(request), request.getHeader("User-Agent"));
        return Result.ok();
    }

    @GetMapping("/summary")
    public Result<AnalyticsSummaryResponse> summary(@RequestParam(defaultValue = "14") Integer days,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) Boolean anonymous) {
        // 登录要求由 SaServletFilter 保证；这里不再限制只能 userId=1 查看。
        return Result.ok(analyticsService.summary(days, userId, anonymous));
    }

    @GetMapping("/recent")
    public Result<List<AnalyticsEvent>> recent(@RequestParam(defaultValue = "100") Integer limit,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) Boolean anonymous) {
        // 登录要求由 SaServletFilter 保证；这里不再限制只能 userId=1 查看。
        return Result.ok(analyticsService.recent(limit, userId, anonymous));
    }

    private Long optionalUserId(String authorization) {
        String token = stripBearer(authorization);
        if (token == null || token.isBlank()) return null;
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stripBearer(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return t.substring(7).trim();
        }
        return t;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
