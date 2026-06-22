package com.panpeixue.myl.model.dto;

import lombok.Data;

@Data
public class AnalyticsEventRequest {
    private String visitorId;
    private String eventType;
    private String path;
    private Object detail;
}
