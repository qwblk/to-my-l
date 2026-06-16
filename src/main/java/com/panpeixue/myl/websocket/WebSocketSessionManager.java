package com.panpeixue.myl.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WebSocketSessionManager {

    /**
     * Tomcat 的 WebSocketSession.sendMessage() 不是线程安全的，并发调用会抛
     * IllegalStateException: TEXT_PARTIAL_WRITING。两个用户在同一毫秒上线时，
     * 各自的 afterConnectionEstablished 线程会同时 broadcast(...)，触发上述异常。
     *
     * 解决：把 raw session 包一层 ConcurrentWebSocketSessionDecorator —— 它内部
     * 用锁 + 出站缓冲，保证「同一个 session」上的发送串行化；不同 session 之间仍并发，
     * 不会拖慢广播。
     *
     * SEND_BUFFER：每个连接的出站缓冲上限（字节）。聊天消息很小，512KB 足够富裕，
     *              超出说明客户端真的卡死了，让 decorator 主动断开是正确行为。
     * SEND_TIMEOUT：单条消息抢锁的最长等待（毫秒）。1 秒抢不到说明对端阻塞了，断它。
     */
    private static final int SEND_BUFFER = 512 * 1024;
    private static final int SEND_TIMEOUT_MS = 1000;

    private static final DateTimeFormatter KICKED_AT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 自定义 close code：被同账号在另一台设备的新连接顶下线。
     * RFC 6455 规定 4000-4999 是应用自定义码段，前端按 event.code === 4001 来识别。
     */
    private static final CloseStatus KICKED = new CloseStatus(4001, "Logged in elsewhere");

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> userMap = new ConcurrentHashMap<>();

    public void add(String username, WebSocketSession session) {
        // kick old session if same user is already online
        kickExisting(username);
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIMEOUT_MS, SEND_BUFFER);
        sessions.put(session.getId(), safe);
        userMap.put(username, session.getId());
    }

    /**
     * 同账号新连接进来时把旧连接踢掉：
     *   1. 先发一帧 kicked，让前端能弹"已在其他设备登录"提示
     *   2. 立即 close(4001)，让前端的 onclose 拿到明确事件码而不是模糊的 1000
     *
     * 失败安静处理 —— 旧连接可能已经断了/网络坏了，不要让踢人流程影响到新连接的 add。
     */
    private void kickExisting(String username) {
        String oldSid = userMap.remove(username);
        if (oldSid == null) return;
        WebSocketSession oldSession = sessions.remove(oldSid);
        if (oldSession == null) return;
        if (oldSession.isOpen()) {
            String at = LocalDateTime.now().format(KICKED_AT_FMT);
            String json = String.format(
                "{\"sender\":\"SYSTEM\",\"content\":\"Logged in elsewhere\","
                + "\"time\":\"%s\",\"type\":\"kicked\",\"data\":{\"at\":\"%s\"}}",
                at.substring(11), at);
            try { oldSession.sendMessage(new TextMessage(json)); } catch (IOException ignored) {}
            try { oldSession.close(KICKED); } catch (IOException ignored) {}
        }
    }

    public void remove(String username, WebSocketSession session) {
        if (session == null) return;
        String sid = session.getId();
        sessions.remove(sid);
        // 旧连接被 kick 后会异步触发 afterConnectionClosed；此时 userMap 可能已经指向新连接。
        // 只能在映射仍指向当前 session 时移除，避免把后来者从在线表里误删。
        if (sid != null && sid.equals(userMap.get(username))) {
            userMap.remove(username);
        }
    }

    /** 测试辅助：清空全部连接状态。生产代码不要调。 */
    void clearAll() {
        sessions.clear();
        userMap.clear();
    }

    public int count() { return userMap.size(); }

    public boolean isOnline(String username) {
        String sid = userMap.get(username);
        if (sid == null) return false;
        WebSocketSession s = sessions.get(sid);
        return s != null && s.isOpen();
    }

    /** current online users */
    public Set<String> onlineUsers() {
        return userMap.keySet().stream()
            .filter(this::isOnline)
            .collect(Collectors.toSet());
    }

    public void broadcast(String json) {
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try { s.sendMessage(msg); } catch (IOException ignored) {}
            }
        }
    }

    public void sendTo(WebSocketSession session, String json) {
        // 调用方传进来的是 Spring 回调里的 raw session；为了和 broadcast 走同一把锁，
        // 这里用它的 id 去 sessions map 里取出 decorator 再发。如果还没 add（理论上
        // 不会发生，因为我们总是 add 之后才 sendTo），就退回到 raw session。
        if (session == null) return;
        WebSocketSession target = sessions.getOrDefault(session.getId(), session);
        send(target, json);
    }

    public void sendToUser(String username, String json) {
        String sid = userMap.get(username);
        if (sid == null) return;
        WebSocketSession target = sessions.get(sid);
        send(target, json);
    }

    private void send(WebSocketSession target, String json) {
        if (target != null && target.isOpen()) {
            try { target.sendMessage(new TextMessage(json)); } catch (IOException ignored) {}
        }
    }
}
