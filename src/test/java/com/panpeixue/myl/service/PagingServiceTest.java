package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.DiaryMapper;
import com.panpeixue.myl.mapper.MessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.DiaryDayGroup;
import com.panpeixue.myl.model.dto.DiaryDaysResponse;
import com.panpeixue.myl.model.dto.MessagePageResponse;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.model.pojo.Message;
import com.panpeixue.myl.service.impl.DiaryServiceImpl;
import com.panpeixue.myl.service.impl.MessageServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagingServiceTest {

    @Mock
    DiaryMapper diaryMapper;

    @Mock
    MessageMapper messageMapper;

    @Mock
    UserMapper userMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @Test
    void diaryDays_scopeMine_onlyUsesMineQueries() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate d1 = LocalDate.of(2026, 6, 14);
        LocalDate d2 = LocalDate.of(2026, 6, 13);
        when(diaryMapper.selectVisibleDatesMine(1L, null, 11)).thenReturn(List.of(d1, d2));
        when(diaryMapper.selectVisibleEntriesByDateMine(1L, d1)).thenReturn(List.of(diary(1L, 1L, d1.atTime(8, 0), 0)));
        when(diaryMapper.selectVisibleEntriesByDateMine(1L, d2)).thenReturn(List.of(diary(2L, 1L, d2.atTime(9, 0), 1)));

        DiaryDaysResponse res = service.listDays(1L, "mine", null, 10);

        assertThat(res.getList()).hasSize(2);
        assertThat(res.getList().get(0).getDate()).isEqualTo("2026-06-14");
        assertThat(res.getList().get(0).getWeekday()).isEqualTo("星期日");
        assertThat(res.getList()).flatExtracting(DiaryDayGroup::getEntries)
            .extracting("userId").containsOnly(1L);
        verify(diaryMapper, never()).selectVisibleDatesAll(anyLong(), any(), anyInt());
        verify(diaryMapper, never()).selectVisibleEntriesByDateAll(anyLong(), any());
    }

    @Test
    void diaryDays_scopeAll_usesAllVisibilityQueriesSoPrivateLeakIsHandledInSql() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate date = LocalDate.of(2026, 6, 14);
        // all 查询的 SQL 是：自己的全部 + 对方 is_private=0。这里模拟 mapper 已按权限过滤后的结果。
        when(diaryMapper.selectVisibleDatesAll(1L, null, 11)).thenReturn(List.of(date));
        when(diaryMapper.selectVisibleEntriesByDateAll(1L, date)).thenReturn(List.of(
            diary(1L, 1L, date.atTime(8, 0), 1), // 自己 private 可见
            diary(2L, 2L, date.atTime(9, 0), 0)  // 对方 public 可见
        ));

        DiaryDaysResponse res = service.listDays(1L, "all", null, 10);

        assertThat(res.getList()).hasSize(1);
        assertThat(res.getList().get(0).getEntries()).extracting("id").containsExactly(1L, 2L);
        verify(diaryMapper).selectVisibleDatesAll(1L, null, 11);
        verify(diaryMapper).selectVisibleEntriesByDateAll(1L, date);
    }

    @Test
    void diaryDays_deletedFilteredByMapper() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate date = LocalDate.of(2026, 6, 14);
        when(diaryMapper.selectVisibleDatesAll(1L, null, 11)).thenReturn(List.of(date));
        when(diaryMapper.selectVisibleEntriesByDateAll(1L, date)).thenReturn(List.of(
            diary(1L, 1L, date.atTime(8, 0), 0)
        ));

        DiaryDaysResponse res = service.listDays(1L, "all", null, 10);

        assertThat(res.getList().get(0).getEntries()).extracting("id").containsExactly(1L);
        verify(diaryMapper).selectVisibleDatesAll(1L, null, 11);
    }

    @Test
    void diaryDays_sameDayMergedAndEntriesKeepMapperAscendingOrder() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate date = LocalDate.of(2026, 6, 14);
        Diary morning = diary(1L, 1L, date.atTime(8, 0), 0);
        Diary evening = diary(2L, 1L, date.atTime(22, 0), 0);
        when(diaryMapper.selectVisibleDatesAll(1L, null, 11)).thenReturn(List.of(date));
        when(diaryMapper.selectVisibleEntriesByDateAll(1L, date)).thenReturn(List.of(morning, evening));

        DiaryDaysResponse res = service.listDays(1L, "all", null, 10);

        assertThat(res.getList()).hasSize(1);
        assertThat(res.getList().get(0).getEntries()).extracting("id").containsExactly(1L, 2L);
    }

    @Test
    void diaryDay_andDiaryDays_scopeAllUseSameVisibilityForSameDate() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate date = LocalDate.of(2026, 6, 22);
        Diary first = diary(1L, 1L, date.atTime(9, 0), 1);
        Diary second = diary(2L, 1L, date.atTime(21, 0), 0);
        when(diaryMapper.selectVisibleDatesAll(1L, null, 11)).thenReturn(List.of(date));
        when(diaryMapper.selectVisibleEntriesByDateAll(1L, date)).thenReturn(List.of(first, second));

        DiaryDaysResponse days = service.listDays(1L, "all", null, 10);
        DiaryDayGroup day = service.getDay(1L, "all", date);

        assertThat(days.getList().get(0).getEntries()).extracting("id").containsExactly(1L, 2L);
        assertThat(day.getEntries()).extracting("id").containsExactly(1L, 2L);
        verify(diaryMapper, times(2)).selectVisibleEntriesByDateAll(1L, date);
    }

    @Test
    void diaryDays_scopeAllForOtherUserDoesNotSeePrivateDiary() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(diaryMapper.selectVisibleDatesAll(2L, null, 11)).thenReturn(List.of(date));
        // mapper 的 all SQL 对 user2 是：user_id=2 的全部 + 其他人的 public；user1 private 不应出现在这里。
        when(diaryMapper.selectVisibleEntriesByDateAll(2L, date)).thenReturn(List.of(
            diary(2L, 1L, date.atTime(10, 0), 0)
        ));

        DiaryDaysResponse days = service.listDays(2L, "all", null, 10);

        assertThat(days.getList().get(0).getEntries()).extracting("id").containsExactly(2L);
        verify(diaryMapper).selectVisibleDatesAll(2L, null, 11);
        verify(diaryMapper).selectVisibleEntriesByDateAll(2L, date);
    }

    @Test
    void diaryDays_cursorDateUsesStrictBeforeAndDoesNotRepeatLastDay() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate cursor = LocalDate.of(2026, 6, 10);
        when(diaryMapper.selectVisibleDatesAll(1L, cursor, 11)).thenReturn(List.of(LocalDate.of(2026, 6, 9)));
        when(diaryMapper.selectVisibleEntriesByDateAll(eq(1L), any())).thenReturn(List.of());

        DiaryDaysResponse res = service.listDays(1L, "all", cursor, 10);

        assertThat(res.getList()).extracting(DiaryDayGroup::getDate).containsExactly("2026-06-09");
        verify(diaryMapper).selectVisibleDatesAll(1L, cursor, 11);
    }

    @Test
    void diaryDay_noDataReturnsEmptyEntries() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        LocalDate date = LocalDate.of(2026, 6, 14);
        when(diaryMapper.selectVisibleEntriesByDateAll(1L, date)).thenReturn(List.of());

        DiaryDayGroup group = service.getDay(1L, "all", date);

        assertThat(group.getDate()).isEqualTo("2026-06-14");
        assertThat(group.getWeekday()).isEqualTo("星期日");
        assertThat(group.getEntries()).isEmpty();
    }

    @Test
    void diaryDays_sizeOverMaxClampedTo30PlusOne() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        when(diaryMapper.selectVisibleDatesAll(eq(1L), isNull(), anyInt())).thenReturn(List.of());

        service.listDays(1L, "all", null, 99);

        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(diaryMapper).selectVisibleDatesAll(eq(1L), isNull(), limit.capture());
        assertThat(limit.getValue()).isEqualTo(31);
    }

    @Test
    void messageReceivedPage_onlyUsesReceiverIdAndBuildsCursor() {
        MessageServiceImpl service = new MessageServiceImpl(messageMapper, userMapper, sessionManager);
        Message m1 = message(1L, 2L, 1L, LocalDateTime.of(2026, 6, 14, 10, 0));
        Message m2 = message(2L, 1L, 1L, LocalDateTime.of(2026, 6, 13, 18, 0));
        when(messageMapper.selectReceivedPage(1L, null, null, 21)).thenReturn(List.of(m1, m2));

        MessagePageResponse page = service.getReceivedPage(1L, null, 20);

        assertThat(page.getList()).extracting("receiverId").containsOnly(1L);
        assertThat(page.getNextCursor()).isEqualTo("2026-06-13 18:00:00");
        assertThat(page.isHasMore()).isFalse();
        verify(messageMapper).selectReceivedPage(1L, null, null, 21);
    }

    @Test
    void messageSentPage_onlyUsesSenderIdAndBuildsCursor() {
        MessageServiceImpl service = new MessageServiceImpl(messageMapper, userMapper, sessionManager);
        Message m1 = message(1L, 1L, 2L, LocalDateTime.of(2026, 6, 14, 10, 0));
        Message m2 = message(2L, 1L, 2L, LocalDateTime.of(2026, 6, 13, 18, 0));
        when(messageMapper.selectSentPage(1L, null, null, 21)).thenReturn(List.of(m1, m2));

        MessagePageResponse page = service.getSentPage(1L, null, 20);

        assertThat(page.getList()).extracting("senderId").containsOnly(1L);
        assertThat(page.getNextCursor()).isEqualTo("2026-06-13 18:00:00");
        verify(messageMapper).selectSentPage(1L, null, null, 21);
    }

    @Test
    void messagePage_cursorUsesStrictBeforeAndNoRepeat() {
        MessageServiceImpl service = new MessageServiceImpl(messageMapper, userMapper, sessionManager);
        LocalDateTime cursor = LocalDateTime.of(2026, 6, 12, 18, 0);
        Message older = message(3L, 2L, 1L, LocalDateTime.of(2026, 6, 12, 17, 59));
        when(messageMapper.selectReceivedPage(1L, cursor, null, 21)).thenReturn(List.of(older));

        MessagePageResponse page = service.getReceivedPage(1L, cursor, 20);

        assertThat(page.getList()).extracting("id").containsExactly(3L);
        verify(messageMapper).selectReceivedPage(1L, cursor, null, 21);
    }

    @Test
    void messagePage_sizeOverMaxClampedTo50PlusOneAndHasMoreDropsExtraRow() {
        MessageServiceImpl service = new MessageServiceImpl(messageMapper, userMapper, sessionManager);
        List<Message> rows = java.util.stream.IntStream.rangeClosed(1, 51)
            .mapToObj(i -> message((long) i, 2L, 1L, LocalDateTime.of(2026, 6, 14, 10, 0).minusMinutes(i)))
            .toList();
        when(messageMapper.selectReceivedPage(1L, null, null, 51)).thenReturn(rows);

        MessagePageResponse page = service.getReceivedPage(1L, null, 99);

        assertThat(page.getList()).hasSize(50);
        assertThat(page.isHasMore()).isTrue();
        verify(messageMapper).selectReceivedPage(1L, null, null, 51);
    }

    private Diary diary(Long id, Long userId, LocalDateTime createTime, int isPrivate) {
        Diary d = new Diary();
        d.setId(id);
        d.setUserId(userId);
        d.setCreateTime(createTime);
        d.setIsPrivate(isPrivate);
        d.setContent("diary " + id);
        return d;
    }

    private Message message(Long id, Long senderId, Long receiverId, LocalDateTime createTime) {
        Message m = new Message();
        m.setId(id);
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setCreateTime(createTime);
        m.setContent("message " + id);
        return m;
    }
}
