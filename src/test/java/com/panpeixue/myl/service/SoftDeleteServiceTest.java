package com.panpeixue.myl.service;

import com.panpeixue.myl.common.BizException;
import com.panpeixue.myl.mapper.DiaryMapper;
import com.panpeixue.myl.mapper.MomentMapper;
import com.panpeixue.myl.model.pojo.Diary;
import com.panpeixue.myl.model.pojo.Moment;
import com.panpeixue.myl.service.impl.DiaryServiceImpl;
import com.panpeixue.myl.service.impl.MomentServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoftDeleteServiceTest {

    @Mock
    MomentMapper momentMapper;

    @Mock
    DiaryMapper diaryMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @Test
    void deleteMoment_ownMoment_success_softDeletesCascadeAndBroadcasts() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(1L);
        when(momentMapper.selectById(10L)).thenReturn(moment);
        when(momentMapper.softDelete(10L)).thenReturn(1);
        when(momentMapper.softDeleteLikesByMomentId(10L)).thenReturn(2);
        when(momentMapper.softDeleteCommentsByMomentId(10L)).thenReturn(3);

        service.delete(10L, 1L);

        verify(momentMapper).softDelete(10L);
        verify(momentMapper).softDeleteLikesByMomentId(10L);
        verify(momentMapper).softDeleteCommentsByMomentId(10L);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).broadcast(json.capture());
        assertThat(json.getValue()).contains("\"sender\":\"SYSTEM\"")
            .contains("\"content\":\"Moment deleted\"")
            .contains("\"type\":\"moment_delete\"")
            .contains("\"momentId\":10")
            .contains("\"userId\":1");
    }

    @Test
    void deleteMoment_otherUser_forbidden403() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(2L);
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.delete(10L, 1L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(403);

        verify(momentMapper, never()).softDelete(anyLong());
        verify(sessionManager, never()).broadcast(anyString());
    }

    @Test
    void deleteMoment_missing_notFound404() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        when(momentMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(404L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("Moment not found")
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void deleteMoment_alreadyDeleted_notFound404() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(1L);
        moment.setDeletedAt(LocalDateTime.now());
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.delete(10L, 1L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void likeDeletedMoment_notFound404() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(2L);
        moment.setDeletedAt(LocalDateTime.now());
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.toggleLike(10L, 1L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void getLikesOfDeletedMoment_notFound404() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(2L);
        moment.setDeletedAt(LocalDateTime.now());
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.getLikes(10L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void commentDeletedMoment_notFound404() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(2L);
        moment.setDeletedAt(LocalDateTime.now());
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.addComment(10L, 1L, "hi"))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void getCommentsOfDeletedMoment_notFound404() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment moment = new Moment();
        moment.setId(10L);
        moment.setUserId(2L);
        moment.setDeletedAt(LocalDateTime.now());
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.getComments(10L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void listAllMoments_usesMapperResultAfterDeletedFilter() {
        MomentServiceImpl service = new MomentServiceImpl(momentMapper, sessionManager);
        Moment visible = new Moment();
        visible.setId(1L);
        visible.setUserId(1L);
        when(momentMapper.selectAll()).thenReturn(List.of(visible));
        when(momentMapper.selectLikesByMomentId(1L)).thenReturn(List.of());
        when(momentMapper.selectCommentsByMomentId(1L)).thenReturn(List.of());
        when(momentMapper.countLikes(1L)).thenReturn(0);

        List<Moment> list = service.listAll();

        assertThat(list).extracting(Moment::getId).containsExactly(1L);
        verify(momentMapper).selectAll();
    }

    @Test
    void deleteDiary_ownDiary_success_softDeletesAndBroadcasts() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = new Diary();
        diary.setId(20L);
        diary.setUserId(1L);
        when(diaryMapper.selectById(20L)).thenReturn(diary);
        when(diaryMapper.softDelete(20L)).thenReturn(1);

        service.delete(20L, 1L);

        verify(diaryMapper).softDelete(20L);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(sessionManager).broadcast(json.capture());
        assertThat(json.getValue()).contains("\"sender\":\"SYSTEM\"")
            .contains("\"content\":\"Diary deleted\"")
            .contains("\"type\":\"diary_delete\"")
            .contains("\"diaryId\":20")
            .contains("\"userId\":1");
    }

    @Test
    void deleteDiary_otherUser_forbidden403() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = new Diary();
        diary.setId(20L);
        diary.setUserId(2L);
        when(diaryMapper.selectById(20L)).thenReturn(diary);

        assertThatThrownBy(() -> service.delete(20L, 1L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(403);

        verify(diaryMapper, never()).softDelete(anyLong());
        verify(sessionManager, never()).broadcast(anyString());
    }

    @Test
    void deleteDiary_missing_notFound404() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        when(diaryMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(404L, 1L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("Diary not found")
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void deleteDiary_alreadyDeleted_notFound404() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary diary = new Diary();
        diary.setId(20L);
        diary.setUserId(1L);
        diary.setDeletedAt(LocalDateTime.now());
        when(diaryMapper.selectById(20L)).thenReturn(diary);

        assertThatThrownBy(() -> service.delete(20L, 1L))
            .isInstanceOf(BizException.class)
            .extracting(e -> ((BizException) e).getCode())
            .isEqualTo(404);
    }

    @Test
    void listAllDiaries_mapperAlreadyFiltersDeletedAndServiceKeepsVisibleOnly() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary visible = new Diary();
        visible.setId(1L);
        visible.setUserId(1L);
        visible.setIsPrivate(0);
        when(diaryMapper.selectAll()).thenReturn(List.of(visible));

        List<Diary> list = service.listAll(2L);

        assertThat(list).extracting(Diary::getId).containsExactly(1L);
        verify(diaryMapper).selectAll();
    }

    @Test
    void listMineDiaries_usesMapperFilteredResult() {
        DiaryServiceImpl service = new DiaryServiceImpl(diaryMapper, sessionManager);
        Diary visible = new Diary();
        visible.setId(1L);
        visible.setUserId(1L);
        when(diaryMapper.selectByUserId(1L)).thenReturn(List.of(visible));

        List<Diary> list = service.listByUser(1L);

        assertThat(list).extracting(Diary::getId).containsExactly(1L);
        verify(diaryMapper).selectByUserId(1L);
    }
}
