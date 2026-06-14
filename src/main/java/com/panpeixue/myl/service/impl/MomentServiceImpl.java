package com.panpeixue.myl.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panpeixue.myl.common.BizException;
import com.panpeixue.myl.mapper.MomentMapper;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.Moment;
import com.panpeixue.myl.model.pojo.MomentComment;
import com.panpeixue.myl.model.pojo.MomentLike;
import com.panpeixue.myl.service.MomentService;
import com.panpeixue.myl.websocket.ChatWebSocketHandler;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class MomentServiceImpl implements MomentService {

    private static final Logger log = LoggerFactory.getLogger(MomentServiceImpl.class);

    /** mediaList 上限 —— 朋友圈惯例 */
    private static final int MAX_MEDIA = 9;

    /** url 必须以这个前缀开头，防止前端塞外站 URL */
    private static final String UPLOAD_URL_PREFIX = "/static/uploads/";

    /** 接受的媒体 type 字面量 */
    private static final Set<String> ALLOWED_TYPES = Set.of("image", "video");

    /**
     * 反序列化 media_list 用的 TypeReference —— 复用同一个对象避免每次 new。
     * Jackson 文档说 TypeReference 实例是线程安全的。
     */
    private static final TypeReference<List<MomentMedia>> MEDIA_LIST_TYPE = new TypeReference<>() {};

    private final MomentMapper momentMapper;
    private final WebSocketSessionManager sessionManager;
    /**
     * 这里手动 new 一个 ObjectMapper 而不是从容器注入：
     * Spring Boot 4 默认走 Jackson 3（tools.jackson），但 pom 里同时显式引入了 Jackson 2，
     * 容器里两套 ObjectMapper 都可能存在；我们用的是 com.fasterxml 的 MomentMedia / @JsonInclude 注解，
     * 必须配 Jackson 2 的 mapper 才稳，自建一个最干净。读写 media_list JSON 字符串都是项目内部数据，
     * 没有时区/日期需求，不需要额外配置。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MomentServiceImpl(MomentMapper momentMapper,
                             WebSocketSessionManager sessionManager) {
        this.momentMapper = momentMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    @CacheEvict(value = "momentList", allEntries = true)
    public Moment create(Long userId, String content, List<MomentMedia> mediaList) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        validateMediaList(mediaList);

        Moment m = new Moment();
        m.setUserId(userId);
        m.setContent(content);
        m.setImage(null);                 // 新发的 Moment 不再写 image 字段
        m.setMediaListJson(serializeMediaList(mediaList));
        momentMapper.insert(m);

        // 给前端的对象也回填 mediaList，省一次往返
        m.setMediaList(mediaList == null ? Collections.emptyList() : mediaList);

        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "New moment posted", "moment", "{\"momentId\":" + m.getId() + ",\"userId\":" + userId + "}"));
        return m;
    }

    @Override
    @Transactional
    @CacheEvict(value = "momentList", allEntries = true)
    public void delete(Long momentId, Long userId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || moment.getDeletedAt() != null) {
            throw BizException.notFound("Moment not found");
        }
        if (!moment.getUserId().equals(userId)) {
            throw BizException.forbidden("No permission");
        }

        momentMapper.softDelete(momentId);
        momentMapper.softDeleteLikesByMomentId(momentId);
        momentMapper.softDeleteCommentsByMomentId(momentId);

        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "Moment deleted", "moment_delete",
            "{\"momentId\":" + momentId + ",\"userId\":" + userId + "}"));
    }

    @Override
    @Cacheable(value = "momentList", key = "'all'", sync = true)
    public List<Moment> listAll() {
        List<Moment> list = momentMapper.selectAll();
        for (Moment m : list) {
            m.setMediaList(resolveMediaList(m));
            m.setLikes(momentMapper.selectLikesByMomentId(m.getId()));
            m.setComments(momentMapper.selectCommentsByMomentId(m.getId()));
            m.setLikeCount(momentMapper.countLikes(m.getId()));
        }
        return list;
    }

    @Override
    public List<MomentLike> getLikes(Long momentId) {
        ensureMomentVisible(momentId);
        return momentMapper.selectLikesByMomentId(momentId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "momentList", allEntries = true)
    public boolean toggleLike(Long momentId, Long userId) {
        Moment moment = getVisibleMomentOr404(momentId);
        if (moment.getUserId().equals(userId)) throw new IllegalArgumentException("Cannot like your own moment");

        if (momentMapper.existsLike(momentId, userId) > 0) {
            momentMapper.deleteLike(momentId, userId);
            sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
                "Like removed", "like", "{\"momentId\":" + momentId + ",\"userId\":" + userId + ",\"liked\":false}"));
            return false;
        }
        MomentLike like = new MomentLike();
        like.setMomentId(momentId);
        like.setUserId(userId);
        momentMapper.insertLike(like);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "New like", "like", "{\"momentId\":" + momentId + ",\"userId\":" + userId + ",\"liked\":true}"));
        return true;
    }

    @Override
    @CacheEvict(value = "momentList", allEntries = true)
    public MomentComment addComment(Long momentId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }
        ensureMomentVisible(momentId);
        MomentComment c = new MomentComment();
        c.setMomentId(momentId);
        c.setUserId(userId);
        c.setContent(content);
        momentMapper.insertComment(c);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "New comment", "comment", "{\"momentId\":" + momentId + ",\"userId\":" + userId + "}"));
        return c;
    }

    @Override
    public List<MomentComment> getComments(Long momentId) {
        ensureMomentVisible(momentId);
        return momentMapper.selectCommentsByMomentId(momentId);
    }

    // ==================== helpers ====================

    private void ensureMomentVisible(Long momentId) {
        getVisibleMomentOr404(momentId);
    }

    private Moment getVisibleMomentOr404(Long momentId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || moment.getDeletedAt() != null) {
            throw BizException.notFound("Moment not found");
        }
        return moment;
    }

    /**
     * 校验 mediaList:
     *   - 可为 null / 空（这次只发文字）
     *   - 不超过 MAX_MEDIA
     *   - 每项 type ∈ {"image","video"}
     *   - 每项 url 必须以 /static/uploads/ 开头（堵外站 URL）
     */
    void validateMediaList(List<MomentMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return;
        if (mediaList.size() > MAX_MEDIA) {
            throw new IllegalArgumentException("mediaList size exceeds " + MAX_MEDIA);
        }
        for (MomentMedia item : mediaList) {
            if (item == null) throw new IllegalArgumentException("mediaList item cannot be null");
            if (!ALLOWED_TYPES.contains(item.getType())) {
                throw new IllegalArgumentException(
                    "mediaList item type must be image or video, got: " + item.getType());
            }
            String url = item.getUrl();
            if (url == null || !url.startsWith(UPLOAD_URL_PREFIX)) {
                throw new IllegalArgumentException(
                    "mediaList item url must start with " + UPLOAD_URL_PREFIX + ", got: " + url);
            }
        }
    }

    /**
     * 兼容老数据 —— 读出 mediaList 给前端:
     *   1) media_list 列有值 → JSON 解析成 List<MomentMedia>
     *   2) 否则 image 列有值 → 包成 [{type:"image", url:image}]
     *   3) 都没有 → 空数组
     */
    List<MomentMedia> resolveMediaList(Moment m) {
        String json = m.getMediaListJson();
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, MEDIA_LIST_TYPE);
            } catch (JsonProcessingException e) {
                // DB 里的 JSON 出错就降级，不要因为脏数据让整页 500
                log.warn("Bad media_list JSON for moment {}: {}", m.getId(), e.getMessage());
            }
        }
        if (m.getImage() != null && !m.getImage().isBlank()) {
            return List.of(new MomentMedia("image", m.getImage(), null, null, null));
        }
        return Collections.emptyList();
    }

    private String serializeMediaList(List<MomentMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(mediaList);
        } catch (JsonProcessingException e) {
            // 校验通过的 mediaList 不应该序列化失败，真出了状况就当代码 bug
            throw new IllegalStateException("Failed to serialize mediaList", e);
        }
    }
}
