package com.panpeixue.myl.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 朋友圈媒体项 —— 一个 Moment 的 mediaList 由多个 MomentMedia 组成。
 * width/height/duration 视类型而定（图片才有 width/height，视频才有 duration），
 * 缺省字段不下发到前端。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MomentMedia {

    /** "image" | "video" */
    private String type;

    /** /static/uploads/2026/06/abc123.jpg */
    private String url;

    /** 图片宽，px；视频可为 null */
    private Integer width;

    /** 图片高，px；视频可为 null */
    private Integer height;

    /** 视频时长，秒；图片为 null */
    private Double duration;
}
