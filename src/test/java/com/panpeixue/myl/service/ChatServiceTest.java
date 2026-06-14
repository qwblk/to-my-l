package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.ChatMessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.ChatHistoryResponse;
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
    void saveChat_persistsTrimmedContent() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(123L);
            return 1;
        });

        ChatMessage saved = service.saveChat(1L, 2L, "  你好  ");

        assertThat(saved.getId()).isEqualTo(123L);
        assertThat(saved.getSenderId()).isEqualTo(1L);
        assertThat(saved.getReceiverId()).isEqualTo(2L);
        assertThat(saved.getContent()).isEqualTo("你好");
        assertThat(saved.getCreateTime()).isNotNull();
        verify(chatMessageMapper).insert(any(ChatMessage.class));
    }

    @Test
    void saveChat_emptyRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);

        assertThatThrownBy(() -> service.saveChat(1L, 2L, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty");
        verifyNoInteractions(chatMessageMapper);
    }

    @Test
    void saveChat_tooLongRejected() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        String tooLong = "x".repeat(501);

        assertThatThrownBy(() -> service.saveChat(1L, 2L, tooLong))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("500");
        verifyNoInteractions(chatMessageMapper);
    }

    @Test
    void history_onlyQueriesCurrentUserAndPartner() {
        ChatServiceImpl service = new ChatServiceImpl(chatMessageMapper, userMapper);
        when(userMapper.selectAll()).thenReturn(List.of(user(1L, "wang"), user(2L, "pan")));
        LocalDateTime t1 = LocalDateTime.of(2026, 6, 14, 22, 10);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 14, 22, 0);
        when(chatMessageMapper.selectHistoryPage(1L, 2L, null, 31)).thenReturn(List.of(
            chat(10L, 1L, 2L, t1), chat(9L, 2L, 1L, t2)
        ));

        ChatHistoryResponse page = service.history(1L, null, 30);

        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList()).allSatisfy(m ->
            assertThat(List.of(m.getSenderId(), m.getReceiverId())).contains(1L, 2L));
        assertThat(page.getNextCursor()).isEqualTo("2026-06-14 22:00:00");
        assertThat(page.isHasMore()).isFalse();
        verify(chatMessageMapper).selectHistoryPage(1L, 2L, null, 31);
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
