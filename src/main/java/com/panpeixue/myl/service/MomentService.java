package com.panpeixue.myl.service;

import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.Moment;
import com.panpeixue.myl.model.pojo.MomentComment;
import com.panpeixue.myl.model.pojo.MomentLike;

import java.util.List;

public interface MomentService {
    Moment create(Long userId, String content, List<MomentMedia> mediaList);
    void delete(Long momentId, Long userId);
    List<Moment> listAll();
    List<MomentLike> getLikes(Long momentId);
    boolean toggleLike(Long momentId, Long userId);
    MomentComment addComment(Long momentId, Long userId, String content);
    List<MomentComment> getComments(Long momentId);
}