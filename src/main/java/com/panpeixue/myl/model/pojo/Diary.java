package com.panpeixue.myl.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Diary implements Serializable {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String mood;
    private String weather;
    private Integer isPrivate = 0;
    private LocalDateTime createTime;
    private String userName;
}