package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.DiaryMapper;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.service.impl.DiaryServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryCreateValidationTest {

    @Mock
    DiaryMapper diaryMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @Test
    void create_defaultsNullIsPrivateToPrivate() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = new Diary();
        diary.setUserId(1L);
        diary.setContent("hello");
        diary.setIsPrivate(null);
        org.mockito.Mockito.doAnswer(inv -> {
            Diary d = inv.getArgument(0);
            d.setId(10L);
            return 1;
        }).when(diaryMapper).insert(diary);
        when(diaryMapper.selectById(10L)).thenReturn(diary);

        service.create(diary);

        assertThat(diary.getIsPrivate()).isEqualTo(1);
        verify(diaryMapper).insert(diary);
        verify(diaryMapper).selectById(10L);
    }

    @Test
    void create_acceptsZeroAndOne() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary publicDiary = diary(0);
        Diary privateDiary = diary(1);

        service.create(publicDiary);
        service.create(privateDiary);

        verify(diaryMapper, times(2)).insert(any(Diary.class));
    }

    @Test
    void create_returnsCompletePersistedDiary() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary input = diary(0);
        org.mockito.Mockito.doAnswer(inv -> {
            Diary d = inv.getArgument(0);
            d.setId(123L);
            return 1;
        }).when(diaryMapper).insert(input);
        Diary saved = diary(0);
        saved.setId(123L);
        saved.setUserName("王水群");
        saved.setCreateTime(java.time.LocalDateTime.of(2026, 6, 22, 18, 30));
        when(diaryMapper.selectById(123L)).thenReturn(saved);

        Diary result = service.create(input);

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getUserName()).isEqualTo("王水群");
        assertThat(result.getCreateTime()).isEqualTo(java.time.LocalDateTime.of(2026, 6, 22, 18, 30));
        assertThat(result.getIsPrivate()).isEqualTo(0);
    }

    @Test
    void create_rejectsInvalidIsPrivate() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = diary(2);

        assertThatThrownBy(() -> service.create(diary))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0 or 1");
        verifyNoInteractions(diaryMapper);
    }

    private Diary diary(Integer isPrivate) {
        Diary d = new Diary();
        d.setUserId(1L);
        d.setContent("hello");
        d.setIsPrivate(isPrivate);
        return d;
    }
}
