package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (sender_name, content, create_time) VALUES (#{senderName}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage msg);

    @Select("SELECT * FROM chat_message ORDER BY create_time LIMIT 100")
    List<ChatMessage> selectRecent();
}