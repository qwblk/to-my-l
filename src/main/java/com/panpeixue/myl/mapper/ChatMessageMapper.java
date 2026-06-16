package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (sender_id, receiver_id, content, media_list, create_time) " +
            "VALUES (#{senderId}, #{receiverId}, #{content}, #{mediaListJson}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage msg);

    @Select("SELECT cm.*, u.name AS sender_name FROM chat_message cm " +
            "LEFT JOIN user u ON cm.sender_id = u.id " +
            "WHERE cm.deleted_at IS NULL " +
            "ORDER BY cm.create_time DESC, cm.id DESC " +
            "LIMIT #{limit}")
    @Results({
        @Result(column = "sender_id", property = "senderId"),
        @Result(column = "receiver_id", property = "receiverId"),
        @Result(column = "sender_name", property = "senderName"),
        @Result(column = "media_list", property = "mediaListJson"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    List<ChatMessage> selectRecent(@Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT cm.*, u.name AS sender_name FROM chat_message cm",
        "LEFT JOIN user u ON cm.sender_id = u.id",
        "WHERE cm.deleted_at IS NULL",
        "AND ((cm.sender_id = #{me} AND cm.receiver_id = #{partner})",
        "  OR (cm.sender_id = #{partner} AND cm.receiver_id = #{me}))",
        "<if test='cursor != null and cursorId != null'>",
        "  AND (cm.create_time &lt; #{cursor} OR (cm.create_time = #{cursor} AND cm.id &lt; #{cursorId}))",
        "</if>",
        "<if test='cursor != null and cursorId == null'>AND cm.create_time &lt; #{cursor}</if>",
        "ORDER BY cm.create_time DESC, cm.id DESC",
        "LIMIT #{limit}",
        "</script>"
    })
    @Results({
        @Result(column = "sender_id", property = "senderId"),
        @Result(column = "receiver_id", property = "receiverId"),
        @Result(column = "sender_name", property = "senderName"),
        @Result(column = "media_list", property = "mediaListJson"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    List<ChatMessage> selectHistoryPage(@Param("me") Long me,
                                        @Param("partner") Long partner,
                                        @Param("cursor") LocalDateTime cursor,
                                        @Param("cursorId") Long cursorId,
                                        @Param("limit") int limit);
}
