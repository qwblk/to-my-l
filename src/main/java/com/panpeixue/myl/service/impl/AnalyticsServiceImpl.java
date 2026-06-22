package com.panpeixue.myl.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panpeixue.myl.mapper.AnalyticsMapper;
import com.panpeixue.myl.model.dto.AnalyticsCountRow;
import com.panpeixue.myl.model.dto.AnalyticsDailyRawCount;
import com.panpeixue.myl.model.dto.AnalyticsEventRequest;
import com.panpeixue.myl.model.dto.AnalyticsSummaryResponse;
import com.panpeixue.myl.model.pojo.AnalyticsEvent;
import com.panpeixue.myl.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int MAX_VISITOR_ID = 64;
    private static final int MAX_EVENT_TYPE = 64;
    private static final int MAX_PATH = 255;
    private static final int MAX_DETAIL = 8192;
    private static final int MAX_IP = 64;
    private static final int MAX_USER_AGENT = 512;
    private static final int DEFAULT_DAYS = 14;
    private static final int MAX_DAYS = 90;
    private static final int DEFAULT_RECENT_LIMIT = 100;
    private static final int MAX_RECENT_LIMIT = 200;
    private static final int SUMMARY_RECENT_LIMIT = 50;

    private static final Set<String> EVENT_TYPES = Set.of(
        "session_start",
        "auth_seen",
        "login_success",
        "page_view",
        "ritual_open",
        "ritual_video_play",
        "ritual_video_end",
        "ritual_hand_view",
        "ritual_enter_click"
    );

    private static final List<String> FUNNEL_TYPES = List.of(
        "ritual_open",
        "ritual_video_play",
        "ritual_video_end",
        "ritual_hand_view",
        "ritual_enter_click",
        "login_success"
    );

    private final AnalyticsMapper analyticsMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyticsServiceImpl(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    @Override
    @Transactional
    public void record(AnalyticsEventRequest req, Long userId, String ip, String userAgent) {
        if (req == null) throw new IllegalArgumentException("Request body is required");

        String eventType = required(req.getEventType(), "eventType", MAX_EVENT_TYPE);
        if (!EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Unsupported eventType: " + eventType);
        }

        AnalyticsEvent event = new AnalyticsEvent();
        event.setUserId(userId);
        event.setVisitorId(optional(req.getVisitorId(), "visitorId", MAX_VISITOR_ID));
        event.setEventType(eventType);
        event.setPath(optional(req.getPath(), "path", MAX_PATH));
        event.setDetail(detailJson(req.getDetail()));
        event.setIp(truncate(blankToNull(ip), MAX_IP));
        event.setUserAgent(truncate(blankToNull(userAgent), MAX_USER_AGENT));

        analyticsMapper.insert(event);
    }

    @Override
    public AnalyticsSummaryResponse summary(Integer days, Long userId, Boolean anonymous) {
        int safeDays = clamp(days, DEFAULT_DAYS, 1, MAX_DAYS);
        AnalyticsFilter filter = filter(userId, anonymous);
        List<AnalyticsCountRow> typeRows = analyticsMapper.countByEventType(safeDays, filter.userId(), filter.anonymous());
        Map<String, Long> byType = toCountMap(typeRows);

        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse();
        response.setDays(safeDays);
        response.setTotalEvents(analyticsMapper.countTotal(safeDays, filter.userId(), filter.anonymous()));
        response.setUniqueVisitors(analyticsMapper.countUniqueVisitors(safeDays, filter.userId(), filter.anonymous()));
        response.setLastVisitAt(analyticsMapper.selectLastVisitAt(safeDays, filter.userId(), filter.anonymous()));
        response.setByEventType(byType);
        response.setFunnel(funnel(byType));
        response.setDaily(daily(analyticsMapper.countDaily(safeDays, filter.userId(), filter.anonymous())));
        response.setRecent(analyticsMapper.selectRecentByDays(safeDays, filter.userId(), filter.anonymous(), SUMMARY_RECENT_LIMIT));
        return response;
    }

    @Override
    public List<AnalyticsEvent> recent(Integer limit, Long userId, Boolean anonymous) {
        AnalyticsFilter filter = filter(userId, anonymous);
        return analyticsMapper.selectRecent(filter.userId(), filter.anonymous(),
            clamp(limit, DEFAULT_RECENT_LIMIT, 1, MAX_RECENT_LIMIT));
    }

    private AnalyticsFilter filter(Long userId, Boolean anonymous) {
        if (Boolean.TRUE.equals(anonymous)) {
            return new AnalyticsFilter(null, true);
        }
        if (userId == null) {
            return new AnalyticsFilter(null, false);
        }
        if (userId != 1L && userId != 2L) {
            throw new IllegalArgumentException("userId must be 1 or 2");
        }
        return new AnalyticsFilter(userId, false);
    }

    private record AnalyticsFilter(Long userId, boolean anonymous) {}

    private String detailJson(Object detail) {
        if (detail == null) return null;
        try {
            String json = objectMapper.writeValueAsString(detail);
            if (json.length() > MAX_DETAIL) {
                throw new IllegalArgumentException("detail is too long");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("detail must be valid JSON");
        }
    }

    private Map<String, Long> toCountMap(List<AnalyticsCountRow> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (AnalyticsCountRow row : rows) {
            if (row.getEventType() != null) {
                map.put(row.getEventType(), row.getCount() == null ? 0L : row.getCount());
            }
        }
        return map;
    }

    private Map<String, Long> funnel(Map<String, Long> byType) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String type : FUNNEL_TYPES) {
            map.put(type, byType.getOrDefault(type, 0L));
        }
        return map;
    }

    private List<AnalyticsSummaryResponse.Daily> daily(List<AnalyticsDailyRawCount> rows) {
        Map<String, AnalyticsSummaryResponse.Daily> byDate = new LinkedHashMap<>();
        for (AnalyticsDailyRawCount row : rows) {
            if (row.getDate() == null) continue;
            AnalyticsSummaryResponse.Daily day = byDate.computeIfAbsent(row.getDate(),
                d -> new AnalyticsSummaryResponse.Daily(d, 0, 0, 0, 0, 0));
            long count = row.getCount() == null ? 0 : row.getCount();
            switch (row.getEventType()) {
                case "session_start" -> day.setSessionStart(count);
                case "auth_seen" -> day.setAuthSeen(count);
                case "login_success" -> day.setLoginSuccess(count);
                case "page_view" -> day.setPageView(count);
                case "ritual_open" -> day.setRitualOpen(count);
                default -> { /* daily 只展示核心字段，其它事件留在 byEventType/funnel */ }
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private String required(String value, String field, int max) {
        String v = blankToNull(value);
        if (v == null) throw new IllegalArgumentException(field + " is required");
        if (v.length() > max) throw new IllegalArgumentException(field + " is too long");
        return v;
    }

    private String optional(String value, String field, int max) {
        String v = blankToNull(value);
        if (v == null) return null;
        if (v.length() > max) throw new IllegalArgumentException(field + " is too long");
        return v;
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private int clamp(Integer value, int def, int min, int max) {
        if (value == null) return def;
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
