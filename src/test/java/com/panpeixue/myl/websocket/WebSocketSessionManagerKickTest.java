package com.panpeixue.myl.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 同账号新连接顶旧连接：
 *   1. 旧连接收到一帧 {"type":"kicked","sender":"SYSTEM",...}
 *   2. 旧连接立即被 close(4001)
 *   3. 新连接保留为当前 sessions/userMap 里的活动连接
 */
@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerKickTest {

    @Mock
    WebSocketSession oldSession;

    @Mock
    WebSocketSession newSession;

    private final WebSocketSessionManager manager = new WebSocketSessionManager();

    @AfterEach
    void cleanUp() {
        // sessions/userMap 是 static，避免不同测试之间残留状态污染。
        // 用直接清 map 的方式，绕开 mock 没设 getId() 的场景。
        manager.clearAll();
    }

    @Test
    void newLogin_kicksOldSession_sendsKickedFrame_andCloses4001() throws IOException {
        when(oldSession.getId()).thenReturn("old-id");
        when(oldSession.isOpen()).thenReturn(true);
        manager.add("alice", oldSession);

        when(newSession.getId()).thenReturn("new-id");
        manager.add("alice", newSession);

        // 1) 发了一帧 type=kicked
        ArgumentCaptor<TextMessage> msgCap = ArgumentCaptor.forClass(TextMessage.class);
        verify(oldSession).sendMessage(msgCap.capture());
        String payload = msgCap.getValue().getPayload();
        assertThat(payload).contains("\"type\":\"kicked\"");
        assertThat(payload).contains("\"sender\":\"SYSTEM\"");
        assertThat(payload).contains("\"content\":\"Logged in elsewhere\"");
        assertThat(payload).contains("\"data\":{\"at\":");

        // 2) close 用了自定义码 4001
        ArgumentCaptor<CloseStatus> closeCap = ArgumentCaptor.forClass(CloseStatus.class);
        verify(oldSession).close(closeCap.capture());
        assertThat(closeCap.getValue().getCode()).isEqualTo(4001);

        // 3) 当前活动连接应该是新的，旧的已经从 sessions 里清掉
        assertThat(manager.isOnline("alice")).isFalse();  // newSession isOpen 默认 false
        // 通过 sendToUser 路由到的是新连接，不会再发到 oldSession
        manager.sendToUser("alice", "{\"type\":\"chat\"}");
        verify(oldSession, atMostOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void firstLogin_doesNotTryToKickAnyone() throws IOException {
        when(newSession.getId()).thenReturn("new-id");
        manager.add("alice", newSession);

        // 没有旧连接，不会发 kicked 帧也不会 close 任何东西
        verify(oldSession, never()).sendMessage(any(TextMessage.class));
        verify(oldSession, never()).close(any(CloseStatus.class));
    }

    @Test
    void newLogin_oldAlreadyClosed_doesNotSendOrCloseAgain() throws IOException {
        when(oldSession.getId()).thenReturn("old-id");
        when(oldSession.isOpen()).thenReturn(false);   // 旧连接已经被网络断了
        manager.add("alice", oldSession);

        when(newSession.getId()).thenReturn("new-id");
        manager.add("alice", newSession);

        verify(oldSession, never()).sendMessage(any(TextMessage.class));
        verify(oldSession, never()).close(any(CloseStatus.class));
    }

    @Test
    void oldConnectionCloseEventDoesNotRemoveNewConnectionMapping() throws IOException {
        when(oldSession.getId()).thenReturn("old-id");
        when(oldSession.isOpen()).thenReturn(true);
        manager.add("alice", oldSession);

        when(newSession.getId()).thenReturn("new-id");
        when(newSession.isOpen()).thenReturn(true);
        manager.add("alice", newSession);

        // 旧连接被 kick 后，Spring 会异步回调 afterConnectionClosed(oldSession)，
        // remove 必须只删 old-id，不能把 userMap 里已经指向 new-id 的映射删掉。
        manager.remove("alice", oldSession);

        assertThat(manager.isOnline("alice")).isTrue();
        manager.sendToUser("alice", "{\"type\":\"chat\"}");
        verify(newSession).sendMessage(any(TextMessage.class));
    }
}
