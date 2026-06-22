package com.panpeixue.myl.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.panpeixue.myl.model.pojo.AnalyticsEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {

    private int days;
    private long totalEvents;
    private long uniqueVisitors;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime lastVisitAt;

    private Map<String, Long> byEventType;
    private Map<String, Long> funnel;
    private List<Daily> daily;
    private List<AnalyticsEvent> recent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Daily {
        private String date;
        private long sessionStart;
        private long authSeen;
        private long loginSuccess;
        private long pageView;
        private long ritualOpen;
    }
}
