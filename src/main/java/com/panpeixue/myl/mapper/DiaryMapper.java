package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.Diary;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DiaryMapper {

    @Insert("INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time) " +
            "VALUES (#{userId}, #{title}, #{content}, #{mood}, #{weather}, #{isPrivate}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Diary diary);

    @Select("SELECT d.*, u.name as user_name FROM diary d LEFT JOIN user u ON d.user_id = u.id " +
            "ORDER BY d.create_time DESC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "is_private", property = "isPrivate"),
        @Result(column = "user_name", property = "userName")
    })
    List<Diary> selectAll();

    @Select("SELECT * FROM diary WHERE user_id = #{userId} ORDER BY create_time DESC")
    @Results(@Result(column = "is_private", property = "isPrivate"))
    List<Diary> selectByUserId(@Param("userId") Long userId);
}