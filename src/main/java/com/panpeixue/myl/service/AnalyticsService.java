package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.AnalyticsEventRequest;
import com.panpeixue.myl.model.dto.AnalyticsSummaryResponse;
import com.panpeixue.myl.model.pojo.AnalyticsEvent;

import java.util.List;

public interface AnalyticsService {

    void record(AnalyticsEventRequest req, Long userId, String ip, String userAgent);

    AnalyticsSummaryResponse summary(Integer days, Long userId, Boolean anonymous);

    List<AnalyticsEvent> recent(Integer limit, Long userId, Boolean anonymous);
}
