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
    /** 本页最后一条消息 id；和 nextCursor 一起用于稳定翻页，避免同秒数据漏掉 */
    private Long nextCursorId;
    private boolean hasMore;

    public ChatHistoryResponse(List<ChatMessage> list, String nextCursor, boolean hasMore) {
        this(list, nextCursor,
            list == null || list.isEmpty() ? null : list.get(list.size() - 1).getId(),
            hasMore);
    }
}
