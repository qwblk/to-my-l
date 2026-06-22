package com.panpeixue.myl.model.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent implements Serializable {

    private Long id;
    private Long userId;
    private String visitorId;
    private String eventType;
    private String path;

    @JsonRawValue
    private String detail;

    private String ip;
    private String userAgent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
