package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.AnalyticsMapper;
import com.panpeixue.myl.model.dto.AnalyticsCountRow;
import com.panpeixue.myl.model.dto.AnalyticsDailyRawCount;
import com.panpeixue.myl.model.dto.AnalyticsEventRequest;
import com.panpeixue.myl.model.dto.AnalyticsSummaryResponse;
import com.panpeixue.myl.model.pojo.AnalyticsEvent;
import com.panpeixue.myl.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    AnalyticsMapper analyticsMapper;

    @Test
    void record_validEvent_serializesDetailAndTruncatesHeaders() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);
        AnalyticsEventRequest req = new AnalyticsEventRequest();
        req.setVisitorId(" visitor-1 ");
        req.setEventType("ritual_open");
        req.setPath("/for-xue");
        req.setDetail(Map.of("step", 1));

        service.record(req, 1L, "1.2.3.4-extra-extra-extra-extra-extra-extra-extra-extra-extra-extra", "u".repeat(600));

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsMapper).insert(captor.capture());
        AnalyticsEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getVisitorId()).isEqualTo("visitor-1");
        assertThat(saved.getEventType()).isEqualTo("ritual_open");
        assertThat(saved.getPath()).isEqualTo("/for-xue");
        assertThat(saved.getDetail()).isEqualTo("{\"step\":1}");
        assertThat(saved.getIp()).hasSize(64);
        assertThat(saved.getUserAgent()).hasSize(512);
    }

    @Test
    void record_rejectsUnsupportedEventType() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);
        AnalyticsEventRequest req = new AnalyticsEventRequest();
        req.setVisitorId("v");
        req.setEventType("not_in_contract");
        req.setPath("/");

        assertThatThrownBy(() -> service.record(req, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported eventType");
    }

    @Test
    void record_rejectsTooLongFieldsAndDetail() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);
        AnalyticsEventRequest req = new AnalyticsEventRequest();
        req.setVisitorId("v".repeat(65));
        req.setEventType("page_view");
        req.setPath("/");

        assertThatThrownBy(() -> service.record(req, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("visitorId is too long");

        req.setVisitorId("v");
        req.setPath("/" + "p".repeat(255));
        assertThatThrownBy(() -> service.record(req, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path is too long");

        req.setPath("/");
        req.setDetail(Map.of("payload", "x".repeat(9000)));
        assertThatThrownBy(() -> service.record(req, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("detail is too long");
    }

    @Test
    void summary_foldsCountsIntoRequestedShape() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);
        when(analyticsMapper.countTotal(14, null, false)).thenReturn(12L);
        when(analyticsMapper.countUniqueVisitors(14, null, false)).thenReturn(2L);
        LocalDateTime last = LocalDateTime.of(2026, 6, 22, 10, 22);
        when(analyticsMapper.selectLastVisitAt(14, null, false)).thenReturn(last);
        when(analyticsMapper.countByEventType(14, null, false)).thenReturn(List.of(
            row("session_start", 3),
            row("page_view", 7),
            row("ritual_open", 2),
            row("ritual_video_play", 1)
        ));
        when(analyticsMapper.countDaily(14, null, false)).thenReturn(List.of(
            daily("2026-06-22", "session_start", 2),
            daily("2026-06-22", "auth_seen", 1),
            daily("2026-06-22", "login_success", 1),
            daily("2026-06-22", "page_view", 8),
            daily("2026-06-22", "ritual_open", 1)
        ));
        when(analyticsMapper.selectRecentByDays(14, null, false, 50)).thenReturn(List.of(new AnalyticsEvent()));

        AnalyticsSummaryResponse res = service.summary(null, null, false);

        assertThat(res.getDays()).isEqualTo(14);
        assertThat(res.getTotalEvents()).isEqualTo(12);
        assertThat(res.getUniqueVisitors()).isEqualTo(2);
        assertThat(res.getLastVisitAt()).isEqualTo(last);
        assertThat(res.getByEventType()).containsEntry("page_view", 7L);
        assertThat(res.getFunnel()).containsEntry("ritual_open", 2L)
                                   .containsEntry("ritual_video_play", 1L)
                                   .containsEntry("ritual_video_end", 0L)
                                   .containsEntry("login_success", 0L);
        assertThat(res.getDaily()).hasSize(1);
        assertThat(res.getDaily().get(0).getDate()).isEqualTo("2026-06-22");
        assertThat(res.getDaily().get(0).getSessionStart()).isEqualTo(2);
        assertThat(res.getDaily().get(0).getAuthSeen()).isEqualTo(1);
        assertThat(res.getDaily().get(0).getLoginSuccess()).isEqualTo(1);
        assertThat(res.getDaily().get(0).getPageView()).isEqualTo(8);
        assertThat(res.getDaily().get(0).getRitualOpen()).isEqualTo(1);
        assertThat(res.getRecent()).hasSize(1);
    }

    @Test
    void recent_clampsLimit() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);
        when(analyticsMapper.selectRecent(null, false, 200)).thenReturn(List.of());

        service.recent(999, null, false);

        verify(analyticsMapper).selectRecent(null, false, 200);
    }

    @Test
    void summary_userFilterAndAnonymousPriorityAreApplied() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);
        when(analyticsMapper.countTotal(14, null, true)).thenReturn(5L);
        when(analyticsMapper.countUniqueVisitors(14, null, true)).thenReturn(2L);
        when(analyticsMapper.countByEventType(14, null, true)).thenReturn(List.of());
        when(analyticsMapper.countDaily(14, null, true)).thenReturn(List.of());
        when(analyticsMapper.selectRecentByDays(14, null, true, 50)).thenReturn(List.of());

        service.summary(14, 2L, true);

        // anonymous=true 优先级高于 userId，所以 userId 被置空，只查 user_id IS NULL。
        verify(analyticsMapper).countTotal(14, null, true);
    }

    @Test
    void summary_rejectsUnknownUserFilter() {
        AnalyticsService service = new AnalyticsServiceImpl(analyticsMapper);

        assertThatThrownBy(() -> service.summary(14, 3L, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId must be 1 or 2");
    }

    private AnalyticsCountRow row(String eventType, long count) {
        AnalyticsCountRow row = new AnalyticsCountRow();
        row.setEventType(eventType);
        row.setCount(count);
        return row;
    }

    private AnalyticsDailyRawCount daily(String date, String eventType, long count) {
        AnalyticsDailyRawCount row = new AnalyticsDailyRawCount();
        row.setDate(date);
        row.setEventType(eventType);
        row.setCount(count);
        return row;
    }
}
