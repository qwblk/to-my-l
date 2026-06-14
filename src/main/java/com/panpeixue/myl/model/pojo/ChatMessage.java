package com.panpeixue.myl.model.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.panpeixue.myl.model.dto.MomentMedia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String content;
    @JsonIgnore
    private String mediaListJson;
    private List<MomentMedia> mediaList;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
    @JsonIgnore
    private LocalDateTime deletedAt;
}
