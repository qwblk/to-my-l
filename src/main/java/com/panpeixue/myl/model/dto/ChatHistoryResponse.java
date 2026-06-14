package com.panpeixue.myl.model.dto;

import com.panpeixue.myl.model.pojo.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    private List<ChatMessage> list;
    /** 本页最后一条消息 createTime：yyyy-MM-dd HH:mm:ss；list 为空时为 null */
    private String nextCursor;
    private boolean hasMore;
}
