package com.panpeixue.myl.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomentCreateRequest {
    private String content;
    private String image;
}