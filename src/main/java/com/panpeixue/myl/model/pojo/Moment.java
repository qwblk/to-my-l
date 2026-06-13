package com.panpeixue.myl.model.pojo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Moment implements Serializable {
    private Long id;
    private Long userId;
    private String content;
    private String image;
    private LocalDateTime createTime;
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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public List<MomentLike> getLikes() { return likes; }
    public void setLikes(List<MomentLike> likes) { this.likes = likes; }
    public List<MomentComment> getComments() { return comments; }
    public void setComments(List<MomentComment> comments) { this.comments = comments; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}