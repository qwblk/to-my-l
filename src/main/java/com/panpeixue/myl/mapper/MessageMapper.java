package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO message (sender_id, receiver_id, content, is_read, create_time) VALUES (#{senderId}, #{receiverId}, #{content}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Select("SELECT m.*, u.name as sender_name FROM message m LEFT JOIN user u ON m.sender_id = u.id WHERE m.receiver_id = #{receiverId} ORDER BY m.create_time DESC")
    @Results(@Result(column = "sender_name", property = "senderName"))
    List<Message> selectByReceiver(@Param("receiverId") Long receiverId);

    @Select("SELECT m.*, u.name as sender_name FROM message m LEFT JOIN user u ON m.sender_id = u.id WHERE m.sender_id = #{senderId} ORDER BY m.create_time DESC")
    @Results(@Result(column = "sender_name", property = "senderName"))
    List<Message> selectBySender(@Param("senderId") Long senderId);

    @Select("SELECT * FROM message WHERE id = #{id}")
    Message selectById(@Param("id") Long id);

    @Update("UPDATE message SET is_read = 1 WHERE id = #{id}")
    int markRead(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{receiverId} AND is_read = 0")
    int countUnread(@Param("receiverId") Long receiverId);
}