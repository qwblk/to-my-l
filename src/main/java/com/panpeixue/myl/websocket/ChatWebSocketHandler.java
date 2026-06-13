package com.panpeixue.myl.websocket;

import com.panpeixue.myl.mapper.ChatMessageMapper;
import com.panpeixue.myl.model.pojo.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter historyFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final WebSocketSessionManager sessionManager;
    private final ChatMessageMapper chatMessageMapper;

    public ChatWebSocketHandler(WebSocketSessionManager sessionManager,
                                ChatMessageMapper chatMessageMapper) {
        this.sessionManager = sessionManager;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = getUsername(session);
        sessionManager.add(username, session);
        log.info("{} connected, online: {}", username, sessionManager.count());

        /* push chat history to the new user */
        List<ChatMessage> history = chatMessageMapper.selectRecent();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append(",");
            ChatMessage m = history.get(i);
            sb.append("{\"sender\":\"").append(esc(m.getSenderName()))
              .append("\",\"content\":\"").append(esc(m.getContent()))
              .append("\",\"time\":\"").append(m.getCreateTime().format(historyFmt))
              .append("\",\"type\":\"chat\"}");
        }
        sb.append("]");
        sessionManager.sendTo(session, buildJson("SYSTEM", "Chat history", "history",
            sb.toString()));

        /* tell everyone this user is now online */
        sessionManager.broadcast(buildJson(username, "is now online", "online", null));

        /* tell THIS user who else is already online */
        Set<String> others = sessionManager.onlineUsers();
        others.remove(username);
        String list = others.isEmpty() ? "" : String.join("\",\"", others);
        sessionManager.sendTo(session, buildJson("SYSTEM", "Current online", "status",
            "{\"online\":[\"" + list + "\"]}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String username = getUsername(session);
        String content = message.getPayload();

        /* persist to DB */
        ChatMessage msg = new ChatMessage();
        msg.setSenderName(username);
        msg.setContent(content);
        chatMessageMapper.insert(msg);

        /* broadcast to all */
        broadcast(username, content, "chat");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = getUsername(session);
        sessionManager.remove(username, session);
        log.info("{} disconnected, online: {}", username, sessionManager.count());
        sessionManager.broadcast(buildJson(username, "is now offline", "offline", null));
    }

    private void broadcast(String sender, String content, String type) {
        sessionManager.broadcast(buildJson(sender, content, type, null));
    }

    public static String buildJson(String sender, String content, String type, String extra) {
        String time = LocalDateTime.now().format(fmt);
        if (extra != null && !extra.isEmpty()) {
            return String.format(
                "{\"sender\":\"%s\",\"content\":\"%s\",\"time\":\"%s\",\"type\":\"%s\",\"data\":%s}",
                esc(sender), esc(content), time, type, extra);
        }
        return String.format(
            "{\"sender\":\"%s\",\"content\":\"%s\",\"time\":\"%s\",\"type\":\"%s\"}",
            esc(sender), esc(content), time, type);
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String getUsername(WebSocketSession session) {
        Object uri = session.getUri();
        if (uri != null) {
            String q = uri.toString();
            if (q.contains("username=")) {
                return q.substring(q.indexOf("username=") + 9);
            }
        }
        return "Anonymous";
    }
}