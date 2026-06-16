package com.panpeixue.myl.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panpeixue.myl.common.BizException;
import com.panpeixue.myl.mapper.ChatMessageMapper;
import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.dto.ChatHistoryResponse;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.ChatMessage;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final int DEFAULT_SIZE = 30;
    private static final int MAX_SIZE = 50;
    private static final int MAX_MEDIA = 9;
    private static final String UPLOAD_URL_PREFIX = "/static/uploads/";
    private static final Set<String> ALLOWED_TYPES = Set.of("image", "video");
    public static final int MAX_CONTENT_LENGTH = 500;
    private static final DateTimeFormatter CURSOR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<List<MomentMedia>> MEDIA_LIST_TYPE = new TypeReference<>() {};

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper, UserMapper userMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public ChatMessage saveChat(Long senderId, Long receiverId, String content, List<MomentMedia> mediaList) {
        String text = normalizeContent(content, mediaList);
        validateMediaList(mediaList);

        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(text);
        msg.setMediaListJson(serializeMediaList(mediaList));
        msg.setMediaList(mediaList == null ? Collections.emptyList() : mediaList);
        msg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);
        return msg;
    }

    @Override
    public ChatHistoryResponse history(Long userId, LocalDateTime cursor, Integer size) {
        return history(userId, cursor, null, size);
    }

    @Override
    public ChatHistoryResponse history(Long userId, LocalDateTime cursor, Long cursorId, Integer size) {
        int pageSize = clampSize(size);
        Long partnerId = findPartnerId(userId);
        List<ChatMessage> rows = chatMessageMapper.selectHistoryPage(userId, partnerId, cursor, cursorId, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<ChatMessage> list = hasMore ? rows.subList(0, pageSize) : rows;
        for (ChatMessage msg : list) {
            msg.setMediaList(resolveMediaList(msg));
        }
        String nextCursor = list.isEmpty() || list.get(list.size() - 1).getCreateTime() == null
            ? null
            : list.get(list.size() - 1).getCreateTime().format(CURSOR_FMT);
        return new ChatHistoryResponse(list, nextCursor, hasMore);
    }

    @Override
    public Long findPartnerId(Long userId) {
        return userMapper.selectAll().stream()
            .map(User::getId)
            .filter(id -> !id.equals(userId))
            .findFirst()
            .orElseThrow(() -> BizException.notFound("Partner not found"));
    }

    private String normalizeContent(String content, List<MomentMedia> mediaList) {
        String text = content == null ? "" : content.trim();
        boolean hasMedia = mediaList != null && !mediaList.isEmpty();
        if (text.isEmpty() && !hasMedia) {
            throw new IllegalArgumentException("Chat content and mediaList cannot both be empty");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Chat content length cannot exceed " + MAX_CONTENT_LENGTH);
        }
        return text;
    }

    private void validateMediaList(List<MomentMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return;
        if (mediaList.size() > MAX_MEDIA) {
            throw new IllegalArgumentException("mediaList size exceeds " + MAX_MEDIA);
        }
        for (MomentMedia item : mediaList) {
            if (item == null) throw new IllegalArgumentException("mediaList item cannot be null");
            if (!ALLOWED_TYPES.contains(item.getType())) {
                throw new IllegalArgumentException("mediaList item type must be image or video, got: " + item.getType());
            }
            String url = item.getUrl();
            if (url == null || !url.startsWith(UPLOAD_URL_PREFIX)) {
                throw new IllegalArgumentException("mediaList item url must start with " + UPLOAD_URL_PREFIX + ", got: " + url);
            }
        }
    }

    private List<MomentMedia> resolveMediaList(ChatMessage msg) {
        String json = msg.getMediaListJson();
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, MEDIA_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Bad chat media_list JSON for message {}: {}", msg.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serializeMediaList(List<MomentMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(mediaList);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize chat mediaList", e);
        }
    }

    private int clampSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}
