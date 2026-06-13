package com.panpeixue.myl.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCreateRequest {
    private String title;
    private String content;
    private String mood;
    private String weather;
    private Integer isPrivate;
}