package com.panpeixue.myl.controller;

import com.panpeixue.myl.common.GlobalExceptionHandler;
import com.panpeixue.myl.model.dto.AnalyticsEventRequest;
import com.panpeixue.myl.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AnalyticsControllerTest {

    private AnalyticsService analyticsService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(analyticsService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void event_allowsAnonymousAndUsesForwardedForFirstIp() throws Exception {
        String body = mvc.perform(post("/analytics/event")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
                .header("X-Real-IP", "9.9.9.9")
                .header("User-Agent", "analytics-test")
                .content("{\"visitorId\":\"v-001\",\"eventType\":\"ritual_open\",\"path\":\"/for-xue\",\"detail\":{\"step\":1}}"))
            .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":200");
        ArgumentCaptor<AnalyticsEventRequest> req = ArgumentCaptor.forClass(AnalyticsEventRequest.class);
        verify(analyticsService).record(req.capture(), eq(null), eq("1.2.3.4"), eq("analytics-test"));
        assertThat(req.getValue().getVisitorId()).isEqualTo("v-001");
        assertThat(req.getValue().getEventType()).isEqualTo("ritual_open");
        assertThat(req.getValue().getPath()).isEqualTo("/for-xue");
    }

    @Test
    void event_invalidTokenDoesNotBlockAnonymousRecord() throws Exception {
        String body = mvc.perform(post("/analytics/event")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid-token")
                .content("{\"visitorId\":\"v-002\",\"eventType\":\"page_view\",\"path\":\"/\"}"))
            .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":200");
        verify(analyticsService).record(any(AnalyticsEventRequest.class), eq(null), any(), any());
    }

    @Test
    void event_validationErrorReturns400Envelope() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("eventType is required"))
            .when(analyticsService).record(any(AnalyticsEventRequest.class), any(), any(), any());

        String body = mvc.perform(post("/analytics/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visitorId\":\"v-003\"}"))
            .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":400");
        assertThat(body).contains("eventType is required");
    }
}
