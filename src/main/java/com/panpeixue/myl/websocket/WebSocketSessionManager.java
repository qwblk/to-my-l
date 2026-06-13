package com.panpeixue.myl.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WebSocketSessionManager {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> userMap = new ConcurrentHashMap<>();

    public void add(String username, WebSocketSession session) {
        sessions.put(session.getId(), session);
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

    /** 当前所有在线的用户名 */
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
        if (session != null && session.isOpen()) {
            try { session.sendMessage(new TextMessage(json)); } catch (IOException ignored) {}
        }
    }
}