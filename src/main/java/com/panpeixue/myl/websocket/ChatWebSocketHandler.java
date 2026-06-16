package com.panpeixue.myl.websocket;

import cn.dev33.satoken.stp.StpUtil;
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

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HEART_TOKEN = "__TML_HEART__";
    private static final CloseStatus UNAUTHORIZED = new CloseStatus(4003, "Unauthorized");
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_USER_ID = "userId";
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
        User user;
        try {
            user = authenticate(session);
        } catch (Exception e) {
            sessionManager.sendTo(session, buildJson("SYSTEM", "Please login first", "error", null));
            try { session.close(UNAUTHORIZED); } catch (Exception ignored) {}
            return;
        }

        String username = user.getUsername();
        session.getAttributes().put(ATTR_USERNAME, username);
        session.getAttributes().put(ATTR_USER_ID, user.getId());
        sessionManager.add(username, session);
        log.info("{} connected, online: {}", username, sessionManager.count());

        pushHistory(session, user.getId());

        /* 上线广播：发给除自己以外的所有人，避免前端被迫"识别这是不是自己"。
         * data.userId 是稳定标识——前端按 ID 路由 presence，不再依赖 displayName 字符串匹配。 */
        sessionManager.broadcastExcept(session, buildJson(
            user.getName(), "is now online", "online",
            "{\"userId\":" + user.getId() + "}"));

        /* 给本人推一份当前在线列表（不含自己）。新协议：data.online 是 [{userId,name}, ...]
         * —— 老协议拼接 String.join 的方式在列表为空时会产出 [""]，前端会被空串误判成"对方在线"。 */
        sessionManager.sendTo(session, buildJson("SYSTEM", "Current online", "status",
            "{\"online\":" + onlineListJson(username) + "}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            sessionManager.sendTo(session, buildJson("SYSTEM", "Please login first", "error", null));
            return;
        }
        User sender = userMapper.selectById(userId);
        if (sender == null) {
            sessionManager.sendTo(session, buildJson("SYSTEM", "Unknown user", "error", null));
            return;
        }
        User receiver = getPartner(sender.getId());
        if (receiver == null) {
            sessionManager.sendTo(session, buildJson("SYSTEM", "Partner not found", "error", null));
            return;
        }

        try {
            Incoming incoming = parse(message.getPayload());
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
        String username = (String) session.getAttributes().get(ATTR_USERNAME);
        if (username == null) return;
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        boolean removedCurrent = sessionManager.remove(username, session);
        log.info("{} disconnected, online: {}", username, sessionManager.count());
        if (removedCurrent) {
            // 离开者的 session 已经在关闭流程里，不必再 except —— 用普通 broadcast 即可。
            // data.userId 让前端无歧义识别"是谁离开了"，老 displayName 路径仍可解析 sender。
            String extra = userId == null ? null : "{\"userId\":" + userId + "}";
            sessionManager.broadcast(buildJson(displayName(username), "is now offline", "offline", extra));
        }
    }

    /**
     * 构造 status 帧的 data.online 数组（不含自己）：
     *   [{"userId":1,"name":"王水群"}, ...]   或者   []
     *
     * 旧实现用 String.join 拼字符串数组，列表为空时会得到 [""]，前端会把空串当成
     * 一个"在线但没名字"的对方，导致 presence 误判。这里用 userId 作为稳定标识，
     * 名字 fallback 到 username（防 sessions map 里残留没设 displayName 的 entry —— 顺手回答了
     * 协议讨论里的"空 slot 是哪儿来的"）。
     *
     * package-private 是为了直接做单元测试，不必走握手路径。
     */
    String onlineListJson(String selfUsername) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String username : sessionManager.onlineUsers()) {
            if (username.equals(selfUsername)) continue;
            User u = userMapper.selectByUsername(username);
            if (u == null) continue;          // session 在但库里查不到，跳过而不是塞空 slot
            if (!first) sb.append(',');
            first = false;
            String name = u.getName() == null || u.getName().isBlank() ? username : u.getName();
            sb.append("{\"userId\":").append(u.getId())
              .append(",\"name\":\"").append(esc(name)).append("\"}");
        }
        sb.append(']');
        return sb.toString();
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
        // heart 不入库，也不需要身份数据；前端只看 type/content/time。
        // 身份如有需要可从 sender 文案判断，data 保持空对象以匹配前端契约。
        return buildJson(sender.getName(), "heart", "heart", "{}");
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

    /**
     * WebSocket 握手鉴权：不再信任 ?username=xxx，必须带有效 token。
     * 支持三种位置，方便浏览器 WebSocket 使用：
     *   1) /ws/chat?token=xxx
     *   2) /ws/chat?Authorization=xxx
     *   3) Header: Authorization: xxx（非浏览器客户端可用）
     */
    private User authenticate(WebSocketSession session) {
        String token = getToken(session);
        Object loginId = StpUtil.getLoginIdByToken(token);
        Long userId = Long.valueOf(String.valueOf(loginId));
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user");
        }
        return user;
    }

    private String getToken(WebSocketSession session) {
        String header = session.getHandshakeHeaders().getFirst("Authorization");
        if (header != null && !header.isBlank()) {
            return stripBearer(header.trim());
        }
        if (session.getUri() != null) {
            String q = session.getUri().getQuery();
            if (q != null) {
                for (String part : q.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq <= 0) continue;
                    String key = part.substring(0, eq);
                    if ("token".equals(key) || "Authorization".equals(key)) {
                        return stripBearer(URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return null;
    }

    private String stripBearer(String token) {
        if (token != null && token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7).trim();
        }
        return token;
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
