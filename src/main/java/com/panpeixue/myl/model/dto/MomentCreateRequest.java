package com.panpeixue.myl.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomentCreateRequest {
    /** 必填，trim 后长度 ≥ 1 —— 校验在 Service 层 */
    private String content;

    /**
     * 多媒体列表，可选。每项需要满足:
     * - type ∈ {"image", "video"}
     * - url 以 "/static/uploads/" 开头（防 SSRF / 外站盗链）
     * 列表最多 9 个（朋友圈惯例）。
     */
    private List<MomentMedia> mediaList;
}
