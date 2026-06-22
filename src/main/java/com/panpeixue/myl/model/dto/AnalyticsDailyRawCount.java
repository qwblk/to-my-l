package com.panpeixue.myl.model.dto;

import lombok.Data;

@Data
public class AnalyticsDailyRawCount {
    private String date;
    private String eventType;
    private Long count;
}
