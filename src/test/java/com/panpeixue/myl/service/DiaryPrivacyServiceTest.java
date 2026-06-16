package com.panpeixue.myl.service;

import com.panpeixue.myl.common.BizException;
import com.panpeixue.myl.mapper.DiaryMapper;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.service.impl.DiaryServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * PUT /diary/{id}/privacy 业务规则：
 *   - 入参 isPrivate 必须是 0 或 1，否则 400
 *   - 不存在/已软删 → 404
 *   - 不是自己的 → 403
 *   - 成功后 mapper.updatePrivacy + WS broadcast type=diary
 */
@ExtendWith(MockitoExtension.class)
class DiaryPrivacyServiceTest {

    @Mock
    DiaryMapper diaryMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @Test
    void updatePrivacy_owner_zeroToOne_success() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = diary(10L, 1L, 0);
        when(diaryMapper.selectById(10L)).thenReturn(diary);
        when(diaryMapper.updatePrivacy(10L, 1)).thenReturn(1);
        Diary updated = diary(10L, 1L, 1);
        when(diaryMapper.selectById(10L)).thenReturn(diary).thenReturn(updated);

        Diary result = service.updatePrivacy(10L, 1L, 1);

        assertThat(result.getIsPrivate()).isEqualTo(1);
        verify(diaryMapper).updatePrivacy(10L, 1);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).broadcast(json.capture());
        assertThat(json.getValue()).contains("\"sender\":\"SYSTEM\"")
            .contains("\"content\":\"Diary updated\"")
            .contains("\"type\":\"diary\"")
            .contains("\"diaryId\":10")
            .contains("\"userId\":1")
            .contains("\"isPrivate\":1");
    }

    @Test
    void updatePrivacy_owner_oneToZero_success() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = diary(10L, 1L, 1);
        when(diaryMapper.selectById(10L)).thenReturn(diary).thenReturn(diary(10L, 1L, 0));
        when(diaryMapper.updatePrivacy(10L, 0)).thenReturn(1);

        Diary result = service.updatePrivacy(10L, 1L, 0);

        assertThat(result.getIsPrivate()).isEqualTo(0);
        verify(diaryMapper).updatePrivacy(10L, 0);
    }

    @Test
    void updatePrivacy_otherOwner_returns403() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        when(diaryMapper.selectById(10L)).thenReturn(diary(10L, 2L, 0));

        assertThatThrownBy(() -> service.updatePrivacy(10L, 1L, 1))
            .isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getCode()).isEqualTo(403));
        verify(diaryMapper, never()).updatePrivacy(anyLong(), anyInt());
        verifyNoInteractions(sessionManager);
    }

    @Test
    void updatePrivacy_notFound_returns404() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        when(diaryMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updatePrivacy(99L, 1L, 1))
            .isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getCode()).isEqualTo(404));
    }

    @Test
    void updatePrivacy_softDeleted_returns404() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary deleted = diary(10L, 1L, 0);
        deleted.setDeletedAt(LocalDateTime.of(2026, 6, 14, 9, 0));
        when(diaryMapper.selectById(10L)).thenReturn(deleted);

        assertThatThrownBy(() -> service.updatePrivacy(10L, 1L, 1))
            .isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getCode()).isEqualTo(404));
    }

    @Test
    void updatePrivacy_invalidIsPrivate_returns400() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);

        assertThatThrownBy(() -> service.updatePrivacy(10L, 1L, 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0 or 1");
        assertThatThrownBy(() -> service.updatePrivacy(10L, 1L, null))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(diaryMapper);
        verifyNoInteractions(sessionManager);
    }

    @Test
    void updatePrivacy_returnedDiaryReflectsNewIsPrivate() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary before = diary(10L, 1L, 0);
        Diary after = diary(10L, 1L, 1);
        when(diaryMapper.selectById(10L)).thenReturn(before).thenReturn(after);
        when(diaryMapper.updatePrivacy(10L, 1)).thenReturn(1);

        Diary result = service.updatePrivacy(10L, 1L, 1);

        assertThat(result.getIsPrivate()).isEqualTo(1);
        verify(diaryMapper, times(2)).selectById(10L);
    }

    private Diary diary(Long id, Long userId, int isPrivate) {
        Diary d = new Diary();
        d.setId(id);
        d.setUserId(userId);
        d.setContent("...");
        d.setIsPrivate(isPrivate);
        return d;
    }
}
