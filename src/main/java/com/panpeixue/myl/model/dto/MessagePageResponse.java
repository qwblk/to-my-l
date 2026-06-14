package com.panpeixue.myl.model.dto;

import com.panpeixue.myl.model.pojo.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePageResponse {
    private List<Message> list;
    /** 本页最后一条消息 createTime：yyyy-MM-dd HH:mm:ss；list 为空时为 null */
    private String nextCursor;
    private boolean hasMore;
}
