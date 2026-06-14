package com.panpeixue.myl.model.dto;

import com.panpeixue.myl.model.pojo.Diary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryDayGroup {
    /** yyyy-MM-dd */
    private String date;
    /** 星期一 / 星期二 ... */
    private String weekday;
    /** 当天日记，按 create_time 正序 */
    private List<Diary> entries;
}
