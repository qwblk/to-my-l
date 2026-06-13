package com.panpeixue.myl.service.impl;

import com.panpeixue.myl.mapper.MomentMapper;
import com.panpeixue.myl.model.pojo.Moment;
import com.panpeixue.myl.model.pojo.MomentComment;
import com.panpeixue.myl.model.pojo.MomentLike;
import com.panpeixue.myl.service.MomentService;
import com.panpeixue.myl.websocket.ChatWebSocketHandler;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MomentServiceImpl implements MomentService {

    private final MomentMapper momentMapper;
    private final WebSocketSessionManager sessionManager;

    public MomentServiceImpl(MomentMapper momentMapper, WebSocketSessionManager sessionManager) {
        this.momentMapper = momentMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    @CacheEvict(value = "momentList", allEntries = true)
    public Moment create(Long userId, String content, String image) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        Moment m = new Moment();
        m.setUserId(userId);
        m.setContent(content);
        m.setImage(image);
        momentMapper.insert(m);
        sessionManager.broadcast(ChatWebSocketHandler.buildJson("SYSTEM",
            "New moment posted", "moment", "{\"momentId\":" + m.getId() + ",\"userId\":" + userId + "}"));
        return m;
    }

    @Override
    @Cacheable(value = "momentList", key = "'all'", sync = true)
    public List<Moment> listAll() {
        List<Moment> list = momentMapper.selectAll();
        for (Moment m : list) {
            m.setLikes(momentMapper.selectLikesByMomentId(m.getId()));
            m.setComments(momentMapper.selectCommentsByMomentId(m.getId()));
            m.setLikeCount(momentMapper.countLikes(m.getId()));
        }
        return list;
    }

    @Override
    public List<MomentLike> getLikes(Long momentId) {
        return momentMapper.selectLikesByMomentId(momentId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "momentList", allEntries = true)
    public boolean toggleLike(Long momentId, Long userId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) throw new IllegalArgumentException("Moment not found");
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
        return momentMapper.selectCommentsByMomentId(momentId);
    }
}