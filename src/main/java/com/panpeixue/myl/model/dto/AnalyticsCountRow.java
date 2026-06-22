package com.panpeixue.myl.model.dto;

import lombok.Data;

@Data
public class AnalyticsCountRow {
    private String eventType;
    private Long count;
}
