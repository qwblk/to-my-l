package com.panpeixue.myl.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * broadcastExcept(self, json) 用于上线广播——发给除自己以外的所有连接。
 *
 * 这个回路修复了前端 presence 长期踩的一个坑：上线广播只要回灌给当事人，
 * 客户端就要做"sender 是不是自己"的字符串识别，而 displayName 在 sessions
 * map 与 auth.user.name 之间并不总能对上。彻底不发给自己，就不存在识别问题。
 */
@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerBroadcastExceptTest {

    @Mock WebSocketSession self;
    @Mock WebSocketSession other;

    private final WebSocketSessionManager manager = new WebSocketSessionManager();

    @AfterEach
    void cleanUp() {
        manager.clearAll();
    }

    @Test
    void broadcastExcept_skipsSelfSession() throws IOException {
        when(self.getId()).thenReturn("self-id");
        when(other.getId()).thenReturn("other-id");
        when(other.isOpen()).thenReturn(true);
        manager.add("alice", self);
        manager.add("bob", other);

        manager.broadcastExcept(self, "{\"type\":\"online\"}");

        // self 的 raw mock 没收到调用——broadcastExcept 是按 session.getId() 过滤的
        verify(self, never()).sendMessage(any(TextMessage.class));
        // other 收到了广播
        verify(other).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastExcept_nullSelf_fallsBackToBroadcastAll() throws IOException {
        when(self.getId()).thenReturn("self-id");
        when(self.isOpen()).thenReturn(true);
        when(other.getId()).thenReturn("other-id");
        when(other.isOpen()).thenReturn(true);
        manager.add("alice", self);
        manager.add("bob", other);

        manager.broadcastExcept(null, "{\"type\":\"online\"}");

        verify(self).sendMessage(any(TextMessage.class));
        verify(other).sendMessage(any(TextMessage.class));
    }
}
