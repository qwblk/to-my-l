package com.panpeixue.myl.websocket;

import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.ChatMessage;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        mockAuthenticated(sender, receiver);
        ChatMessage saved = chat(123L, 1L, 2L, "你好");
        when(chatService.saveChat(1L, 2L, "你好", null)).thenReturn(saved);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"chat\",\"content\":\"你好\"}"));

        verify(chatService).saveChat(1L, 2L, "你好", null);
        verify(sessionManager).sendToUser(eq("wangshuiqun"), contains("\"type\":\"chat\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"chat\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"id\":123"));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"mediaList\":[]"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void jsonChat_withMedia_passesMediaToServiceAndBroadcastsMediaList() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        mockAuthenticated(sender, receiver);
        ChatMessage saved = chat(124L, 1L, 2L, "");
        saved.setMediaList(List.of(
            new MomentMedia("image", "/static/uploads/2026/06/a.jpg", 1024, 768, null),
            new MomentMedia("video", "/static/uploads/2026/06/b.mp4", null, null, 12.5)));
        when(chatService.saveChat(eq(1L), eq(2L), eq(""), any(List.class))).thenReturn(saved);

        String payload = "{\"type\":\"chat\",\"content\":\"\",\"mediaList\":["
            + "{\"type\":\"image\",\"url\":\"/static/uploads/2026/06/a.jpg\",\"width\":1024,\"height\":768},"
            + "{\"type\":\"video\",\"url\":\"/static/uploads/2026/06/b.mp4\",\"duration\":12.5}"
            + "]}";
        handler.handleTextMessage(session, new TextMessage(payload));

        ArgumentCaptor<List<MomentMedia>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatService).saveChat(eq(1L), eq(2L), eq(""), captor.capture());
        List<MomentMedia> sent = captor.getValue();
        assertThat(sent).hasSize(2);
        assertThat(sent.get(0).getType()).isEqualTo("image");
        assertThat(sent.get(0).getUrl()).isEqualTo("/static/uploads/2026/06/a.jpg");
        assertThat(sent.get(0).getWidth()).isEqualTo(1024);
        assertThat(sent.get(1).getType()).isEqualTo("video");
        assertThat(sent.get(1).getDuration()).isEqualTo(12.5);

        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"mediaList\":["));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("/static/uploads/2026/06/a.jpg"));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("/static/uploads/2026/06/b.mp4"));
    }

    @Test
    void jsonHeart_doesNotSaveDbAndBroadcastsHeart() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        mockAuthenticated(sender, receiver);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"heart\"}"));

        verifyNoInteractions(chatService);
        verify(sessionManager).sendToUser(eq("wangshuiqun"), contains("\"type\":\"heart\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"heart\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"content\":\"heart\""));
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"data\":{}"));
    }

    @Test
    void legacyHeartToken_doesNotSaveDbAndIsNotChat() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        mockAuthenticated(sender, receiver);

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
        mockAuthenticated(sender, receiver);
        when(chatService.saveChat(1L, 2L, "老文本", null))
            .thenReturn(chat(125L, 1L, 2L, "老文本"));

        handler.handleTextMessage(session, new TextMessage("老文本"));

        verify(chatService).saveChat(1L, 2L, "老文本", null);
        verify(sessionManager).sendToUser(eq("panpeixue"), contains("\"type\":\"chat\""));
    }

    @Test
    void emptyChatRejectedAndNotSaved() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        mockAuthenticated(sender, receiver);
        when(chatService.saveChat(1L, 2L, "   ", null))
            .thenThrow(new IllegalArgumentException("Chat content and mediaList cannot both be empty"));

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"chat\",\"content\":\"   \"}"));

        verify(chatService).saveChat(1L, 2L, "   ", null);
        verify(sessionManager).sendTo(eq(session), contains("\"type\":\"error\""));
    }

    @Test
    void unsupportedJsonCommandReturnsErrorFrame() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User sender = user(1L, "wangshuiqun", "王水群");
        User receiver = user(2L, "panpeixue", "潘佩雪");
        mockAuthenticated(sender, receiver);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"unknown\"}"));

        verifyNoInteractions(chatService);
        verify(sessionManager).sendTo(eq(session), contains("\"type\":\"error\""));
    }

    /* ------------------------------------------------------------------------
     * 新 presence 协议（按 userId 路由，不再依赖 displayName 字符串匹配）：
     *   - status.data.online 是 [{userId,name}, ...]，单 / 空场景都不再产出 [""]
     *   - online / offline 帧 data 里带 userId，前端可直接按 ID 过滤
     * ------------------------------------------------------------------------ */
    @Test
    void onlineList_emptyWhenAlone() {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        when(sessionManager.onlineUsers()).thenReturn(java.util.Set.of("wangshuiqun"));
        // 自己不在结果里，userMapper 也就不会被查；不必 stub。

        String json = handler.onlineListJson("wangshuiqun");

        // 关键回归点：旧实现会得到 [""]，新实现必须是 []。
        assertThat(json).isEqualTo("[]");
    }

    @Test
    void onlineList_emitsObjectWithUserIdAndName() {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        User other = user(2L, "panpeixue", "潘佩雪");
        when(sessionManager.onlineUsers())
            .thenReturn(java.util.Set.of("wangshuiqun", "panpeixue"));
        when(userMapper.selectByUsername("panpeixue")).thenReturn(other);

        String json = handler.onlineListJson("wangshuiqun");

        assertThat(json)
            .contains("\"userId\":2")
            .contains("\"name\":\"潘佩雪\"")
            .doesNotContain("\"\""); // 不能有任何裸空串 slot
    }

    @Test
    void onlineList_skipsEntryWhenUserMissingFromDb() {
        // 极端情形：sessions map 里有 username 但库里查不到（被删号但 ws 还活着）
        // 旧协议会塞一个空 slot 进去；新协议必须直接跳过。
        ChatWebSocketHandler handler = new ChatWebSocketHandler(sessionManager, chatService, userMapper);
        when(sessionManager.onlineUsers())
            .thenReturn(java.util.Set.of("wangshuiqun", "ghost"));
        when(userMapper.selectByUsername("ghost")).thenReturn(null);

        String json = handler.onlineListJson("wangshuiqun");

        assertThat(json).isEqualTo("[]");
    }

    private void mockAuthenticated(User sender, User receiver) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", sender.getId());
        attrs.put("username", sender.getUsername());
        when(session.getAttributes()).thenReturn(attrs);
        when(userMapper.selectById(sender.getId())).thenReturn(sender);
        when(userMapper.selectAll()).thenReturn(List.of(sender, receiver));
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
