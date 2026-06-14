package com.panpeixue.myl.websocket;

import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.pojo.ChatMessage;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    @Mock
    WebSocketSessionManager sessionManager;

    @Mock
    ChatService chatService;

    @Mock
    UserMapper userMapper;

    @Mock
    WebSocketSession session;

    @Test
    void jsonChat_savesDbAndBroadcastsToBothUsers() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8081/ws/chat?username=wangshuiqun"));
        when(userMapper.selectByUsername("wangshuiqun")).thenReturn(sender);
        when(userMapper.selectAll()).thenReturn(List.of(sender, receiver));
        ChatMessage saved = chat(123L, 1L, 2L, "你好");
        when(chatService.saveChat(1L, 2L, "你好")).thenReturn(saved);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"chat\",\"content\":\"你好\"}"));

        verify(chatService).saveChat(1L, 2L, "你好");
        verify(sessionManager).sendToUser(eq("wangshuiqun"), contains("\"type\":\"chat\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"chat\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"id\":123"));
    }

    @Test
    void jsonHeart_doesNotSaveDbAndBroadcastsHeart() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8081/ws/chat?username=wangshuiqun"));
        when(userMapper.selectByUsername("wangshuiqun")).thenReturn(sender);
        when(userMapper.selectAll()).thenReturn(List.of(sender, receiver));

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"heart\"}"));

        verifyNoInteractions(chatService);
        verify(sessionManager).sendToUser(eq("wangshuiqun"), contains("\"type\":\"heart\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"heart\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"content\":\"heart\""));
    }

    @Test
    void legacyHeartToken_doesNotSaveDbAndIsNotChat() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8081/ws/chat?username=wangshuiqun"));
        when(userMapper.selectByUsername("wangshuiqun")).thenReturn(sender);
        when(userMapper.selectAll()).thenReturn(List.of(sender, receiver));

        handler.handleTextMessage(session, new TextMessage("__TML_HEART__"));

        verifyNoInteractions(chatService);
        verify(sessionManager, never()).sendToUser(anyString(), contains("\"type\":\"chat\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"heart\""));
    }

    @Test
    void legacyPlainText_savesAsChat() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8081/ws/chat?username=wangshuiqun"));
        when(userMapper.selectByUsername("wangshuiqun")).thenReturn(sender);
        when(userMapper.selectAll()).thenReturn(List.of(sender, receiver));
        when(chatService.saveChat(eq(1L), eq(2L), eq("老文本")))
            .thenReturn(chat(124L, 1L, 2L, "老文本"));

        handler.handleTextMessage(session, new TextMessage("老文本"));

        verify(chatService).saveChat(1L, 2L, "老文本");
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"chat\""));
    }

    @Test
    void emptyChatRejectedAndNotSaved() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8081/ws/chat?username=wangshuiqun"));
        when(userMapper.selectByUsername("wangshuiqun")).thenReturn(sender);
        when(userMapper.selectAll()).thenReturn(List.of(sender, receiver));
        when(chatService.saveChat(1L, 2L, "   ")).thenThrow(new IllegalArgumentException("Chat content cannot be empty"));

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"chat\",\"content\":\"   \"}"));

        verify(chatService).saveChat(1L, 2L, "   ");
        verify(sessionManager).sendTo(eq(session), contains("\"type\":\"error\""));
    }

    private User user(Long id, String username, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName(name);
        return u;
    }

    private ChatMessage chat(Long id, Long senderId, Long receiverId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setCreateTime(LocalDateTime.of(2026, 6, 14, 22, 10, 3));
        return msg;
    }
}
