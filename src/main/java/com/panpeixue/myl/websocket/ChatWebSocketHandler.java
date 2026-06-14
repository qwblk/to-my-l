package com.panpeixue.myl.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.ChatMessage;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HEART_TOKEN = "__TML_HEART__";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<MomentMedia>> MEDIA_LIST_TYPE = new TypeReference<>() {};

    private final WebSocketSessionManager sessionManager;
    private final ChatService chatService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(WebSocketSessionManager sessionManager,
                                ChatService chatService,
                                UserMapper userMapper) {
        this.sessionManager = sessionManager;
        this.chatService = chatService;
        this.userMapper = userMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = getUsername(session);
        sessionManager.add(username, session);
        log.info("{} connected, online: {}", username, sessionManager.count());

        User user = userMapper.selectByUsername(username);
        if (user != null) {
            pushHistory(session, user.getId());
        }

        /* tell everyone this user is now online */
        sessionManager.broadcast(buildJson(displayName(username), "is now online", "online", null));

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
        User sender = userMapper.selectByUsername(username);
        if (sender == null) {
            sessionManager.sendTo(session, buildJson("SYSTEM", "Unknown user", "error", null));
            return;
        }
        User receiver = getPartner(sender.getId());
        if (receiver == null) {
            sessionManager.sendTo(session, buildJson("SYSTEM", "Partner not found", "error", null));
            return;
        }

        Incoming incoming = parse(message.getPayload());
        try {
            if (incoming.isHeart()) {
                broadcastToPair(sender, receiver, buildHeartJson(sender, receiver));
                return;
            }

            ChatMessage saved = chatService.saveChat(sender.getId(), receiver.getId(),
                incoming.content(), incoming.mediaList());
            String json = buildChatJson(sender, receiver, saved);
            broadcastToPair(sender, receiver, json);
        } catch (IllegalArgumentException e) {
            sessionManager.sendTo(session, buildJson("SYSTEM", e.getMessage(), "error", null));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = getUsername(session);
        sessionManager.remove(username, session);
        log.info("{} disconnected, online: {}", username, sessionManager.count());
        sessionManager.broadcast(buildJson(displayName(username), "is now offline", "offline", null));
    }

    private void pushHistory(WebSocketSession session, Long userId) {
        ChatHistoryResponse history = chatService.history(userId, null, 30);
        StringBuilder messages = new StringBuilder("[");
        List<ChatMessage> list = history.getList();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) messages.append(',');
            messages.append(toChatMessageJson(list.get(i)));
        }
        messages.append(']');
        sessionManager.sendTo(session, buildJson("SYSTEM", "history", "history",
            "{\"messages\":" + messages + "}"));
    }

    private void broadcastToPair(User sender, User receiver, String json) {
        sessionManager.sendToUser(sender.getUsername(), json);
        sessionManager.sendToUser(receiver.getUsername(), json);
    }

    private String buildChatJson(User sender, User receiver, ChatMessage saved) {
        StringBuilder data = new StringBuilder("{")
            .append("\"id\":").append(saved.getId())
            .append(",\"senderId\":").append(sender.getId())
            .append(",\"receiverId\":").append(receiver.getId())
            .append(",\"createTime\":\"").append(saved.getCreateTime().format(dateTimeFmt)).append("\"")
            .append(",\"mediaList\":").append(mediaListJson(saved.getMediaList()))
            .append("}");
        return buildJson(sender.getName(), saved.getContent(), "chat", data.toString());
    }

    private String buildHeartJson(User sender, User receiver) {
        String data = "{\"senderId\":" + sender.getId()
            + ",\"receiverId\":" + receiver.getId() + "}";
        return buildJson(sender.getName(), "heart", "heart", data);
    }

    private String toChatMessageJson(ChatMessage m) {
        return "{\"id\":" + m.getId()
            + ",\"senderId\":" + m.getSenderId()
            + ",\"receiverId\":" + m.getReceiverId()
            + ",\"senderName\":\"" + esc(m.getSenderName()) + "\""
            + ",\"content\":\"" + esc(m.getContent()) + "\""
            + ",\"createTime\":\"" + m.getCreateTime().format(dateTimeFmt) + "\""
            + ",\"mediaList\":" + mediaListJson(m.getMediaList())
            + "}";
    }

    /**
     * 把 List<MomentMedia> 序列化成 JSON 字符串嵌进广播 / 历史响应里。
     * 失败时降级成 []，避免单条脏数据让整条 WS 消息坏掉。
     */
    private String mediaListJson(List<MomentMedia> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private Incoming parse(String payload) {
        if (payload == null) return Incoming.chat("", null);
        String trimmed = payload.trim();
        if (HEART_TOKEN.equals(trimmed)) return Incoming.heartEvent();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, Object> obj = objectMapper.readValue(trimmed, MAP_TYPE);
                Object type = obj.get("type");
                if ("heart".equals(type)) return Incoming.heartEvent();
                if ("chat".equals(type)) {
                    String content = obj.get("content") == null ? "" : String.valueOf(obj.get("content"));
                    Object rawMedia = obj.get("mediaList");
                    List<MomentMedia> media = null;
                    if (rawMedia != null) {
                        // Map -> POJO 用 convertValue，不用再走一次序列化
                        media = objectMapper.convertValue(rawMedia, MEDIA_LIST_TYPE);
                    }
                    return Incoming.chat(content, media);
                }
                throw new IllegalArgumentException("Unsupported chat command type: " + type);
            } catch (JsonProcessingException e) {
                // JSON 解析失败时按旧前端纯文本处理，最大兼容
                return Incoming.chat(payload, null);
            } catch (IllegalArgumentException e) {
                // convertValue 在结构不对时会抛 IAE：mediaList 字段格式错就直接报错
                if (e.getMessage() != null && e.getMessage().startsWith("Unsupported chat")) throw e;
                throw new IllegalArgumentException("Invalid mediaList payload");
            }
        }
        return Incoming.chat(payload, null);
    }

    private User getPartner(Long userId) {
        return userMapper.selectAll().stream()
            .filter(u -> !u.getId().equals(userId))
            .findFirst()
            .orElse(null);
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
        if (session.getUri() != null) {
            String q = session.getUri().getQuery();
            if (q != null) {
                for (String part : q.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq > 0 && "username".equals(part.substring(0, eq))) {
                        return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                    }
                }
            }
        }
        return "Anonymous";
    }

    private String displayName(String username) {
        User user = userMapper.selectByUsername(username);
        return user == null ? username : user.getName();
    }

    private record Incoming(boolean isHeart, String content, List<MomentMedia> mediaList) {
        static Incoming heartEvent() { return new Incoming(true, null, null); }
        static Incoming chat(String content, List<MomentMedia> mediaList) {
            return new Incoming(false, content, mediaList);
        }
    }
}
