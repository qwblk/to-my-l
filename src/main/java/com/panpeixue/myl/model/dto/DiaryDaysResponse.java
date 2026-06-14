package com.panpeixue.myl.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryDaysResponse {
    private List<DiaryDayGroup> list;
    /** 本页最后一天 yyyy-MM-dd；list 为空时为 null */
    private String nextCursorDate;
    private boolean hasMore;
}
