package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.ChatMessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.ChatMessage;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    ChatMessageMapper chatMessageMapper;

    @Mock
    UserMapper userMapper;

    @Test
    void saveChat_persistsTrimmedTextWithoutMedia() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(123L);
            return 1;
        });

        ChatMessage saved = service.saveChat(1L, 2L, "  你好  ", null);

        assertThat(saved.getId()).isEqualTo(123L);
        assertThat(saved.getContent()).isEqualTo("你好");
        assertThat(saved.getMediaListJson()).isNull();
        assertThat(saved.getMediaList()).isEmpty();
    }

    @Test
    void saveChat_emptyContentAndEmptyMediaRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);

        assertThatThrownBy(() -> service.saveChat(1L, 2L, "   ", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty");
        assertThatThrownBy(() -> service.saveChat(1L, 2L, null, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(chatMessageMapper);
    }

    @Test
    void saveChat_emptyContentWithMediaIsAllowed() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(200L);
            return 1;
        });
        List<MomentMedia> media = List.of(
            new MomentMedia("image", "/static/uploads/2026/06/a.jpg", 1024, 768, null));

        ChatMessage saved = service.saveChat(1L, 2L, "", media);

        assertThat(saved.getContent()).isEmpty();
        assertThat(saved.getMediaListJson())
            .startsWith("[")
            .contains("\"type\":\"image\"")
            .contains("/static/uploads/");
        assertThat(saved.getMediaList()).hasSize(1);
    }

    @Test
    void saveChat_externalMediaUrlRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        List<MomentMedia> bad = List.of(
            new MomentMedia("image", "http://evil.com/x.jpg", null, null, null));

        assertThatThrownBy(() -> service.saveChat(1L, 2L, "hi", bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("/static/uploads/");
        verifyNoInteractions(chatMessageMapper);
    }

    @Test
    void saveChat_unknownMediaTypeRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        List<MomentMedia> bad = List.of(
            new MomentMedia("audio", "/static/uploads/x.mp3", null, null, null));

        assertThatThrownBy(() -> service.saveChat(1L, 2L, "hi", bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("image or video");
    }

    @Test
    void saveChat_tooManyMediaRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        List<MomentMedia> tooMany = java.util.stream.IntStream.range(0, 10)
            .mapToObj(i -> new MomentMedia("image", "/static/uploads/" + i + ".jpg", null, null, null))
            .toList();

        assertThatThrownBy(() -> service.saveChat(1L, 2L, "hi", tooMany))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("9");
    }

    @Test
    void saveChat_tooLongTextRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        String tooLong = "x".repeat(501);

        assertThatThrownBy(() -> service.saveChat(1L, 2L, tooLong, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("500");
        verifyNoInteractions(chatMessageMapper);
    }

    @Test
    void history_resolvesMediaListFromJson() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(userMapper.selectAll()).thenReturn(List.of(user(1L, "wang"), user(2L, "pan")));
        ChatMessage row = new ChatMessage();
        row.setId(99L);
        row.setSenderId(1L);
        row.setReceiverId(2L);
        row.setContent("");
        row.setCreateTime(LocalDateTime.of(2026, 6, 14, 22, 10));
        row.setMediaListJson(
            "[{\"type\":\"image\",\"url\":\"/static/uploads/2026/06/a.jpg\",\"width\":800}]");
        when(chatMessageMapper.selectHistoryPage(1L, 2L, null, 31)).thenReturn(List.of(row));

        ChatHistoryResponse page = service.history(1L, null, 30);

        assertThat(page.getList()).hasSize(1);
        List<MomentMedia> media = page.getList().get(0).getMediaList();
        assertThat(media).hasSize(1);
        assertThat(media.get(0).getType()).isEqualTo("image");
        assertThat(media.get(0).getUrl()).isEqualTo("/static/uploads/2026/06/a.jpg");
        assertThat(media.get(0).getWidth()).isEqualTo(800);
    }

    @Test
    void history_corruptMediaJsonFallsBackToEmpty() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(userMapper.selectAll()).thenReturn(List.of(user(1L, "wang"), user(2L, "pan")));
        ChatMessage row = new ChatMessage();
        row.setId(100L);
        row.setSenderId(1L);
        row.setReceiverId(2L);
        row.setContent("text");
        row.setCreateTime(LocalDateTime.of(2026, 6, 14, 22, 10));
        row.setMediaListJson("{not json");
        when(chatMessageMapper.selectHistoryPage(1L, 2L, null, 31)).thenReturn(List.of(row));

        ChatHistoryResponse page = service.history(1L, null, 30);

        assertThat(page.getList().get(0).getMediaList()).isEmpty();
    }

    @Test
    void history_cursorAndSizeClamp() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(userMapper.selectAll()).thenReturn(List.of(user(1L, "wang"), user(2L, "pan")));
        LocalDateTime cursor = LocalDateTime.of(2026, 6, 12, 18, 0);
        List<ChatMessage> rows = java.util.stream.IntStream.rangeClosed(1, 51)
            .mapToObj(i -> chat((long) i, 1L, 2L, cursor.minusMinutes(i)))
            .toList();
        when(chatMessageMapper.selectHistoryPage(1L, 2L, cursor, 51)).thenReturn(rows);

        ChatHistoryResponse page = service.history(1L, cursor, 99);

        assertThat(page.getList()).hasSize(50);
        assertThat(page.isHasMore()).isTrue();
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(chatMessageMapper).selectHistoryPage(eq(1L), eq(2L), eq(cursor), limit.capture());
        assertThat(limit.getValue()).isEqualTo(51);
    }

    private User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName(username);
        return u;
    }

    private ChatMessage chat(Long id, Long senderId, Long receiverId, LocalDateTime createTime) {
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setSenderName("sender" + senderId);
        msg.setContent("msg" + id);
        msg.setCreateTime(createTime);
        return msg;
    }
}
