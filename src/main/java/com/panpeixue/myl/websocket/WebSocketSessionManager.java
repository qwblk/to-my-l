package com.panpeixue.myl.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
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

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> userMap = new ConcurrentHashMap<>();

    public void add(String username, WebSocketSession session) {
        // kick old session if same user is already online
        String oldSid = userMap.get(username);
        if (oldSid != null) {
            WebSocketSession oldSession = sessions.get(oldSid);
            if (oldSession != null && oldSession.isOpen()) {
                try { oldSession.close(CloseStatus.NORMAL); } catch (IOException ignored) {}
            }
            sessions.remove(oldSid);
        }
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIMEOUT_MS, SEND_BUFFER);
        sessions.put(session.getId(), safe);
        userMap.put(username, session.getId());
    }

    public void remove(String username, WebSocketSession session) {
        sessions.remove(session.getId());
        userMap.remove(username);
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
