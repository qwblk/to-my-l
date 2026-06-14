package com.panpeixue.myl.model.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.panpeixue.myl.model.dto.MomentMedia;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Moment implements Serializable {
    private Long id;
    private Long userId;
    private String content;
    /** @deprecated 老数据兼容字段，新发的 Moment 用 mediaList */
    @Deprecated
    private String image;
    /**
     * MyBatis 直接读写的列（DB 里是 TEXT 存 JSON 字符串）。
     * 不下发到前端 —— 前端只看 mediaList。
     */
    @JsonIgnore
    private String mediaListJson;
    /** 给前端的多媒体数组。Service 层从 mediaListJson / image 兼容老数据后填充。 */
    private List<MomentMedia> mediaList;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
    @JsonIgnore
    private LocalDateTime deletedAt;
    /* 以下为联表查询填充 */
    private String userName;
    private List<MomentLike> likes;
    private List<MomentComment> comments;
    private int likeCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getMediaListJson() { return mediaListJson; }
    public void setMediaListJson(String mediaListJson) { this.mediaListJson = mediaListJson; }
    public List<MomentMedia> getMediaList() { return mediaList; }
    public void setMediaList(List<MomentMedia> mediaList) { this.mediaList = mediaList; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public List<MomentLike> getLikes() { return likes; }
    public void setLikes(List<MomentLike> likes) { this.likes = likes; }
    public List<MomentComment> getComments() { return comments; }
    public void setComments(List<MomentComment> comments) { this.comments = comments; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}