package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT id, name, gender, username, birthday, bio, is_first_login, last_seen_at, create_time, update_time FROM user WHERE id = #{id}")
    @Results({@Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "is_first_login", property = "isFirstLogin"),
        @Result(column = "last_seen_at", property = "lastSeenAt")})
    User selectById(@Param("id") Long id);

    @Select("SELECT id, name, gender, username, birthday, bio, is_first_login, last_seen_at, create_time, update_time FROM user WHERE username = #{username}")
    @Results({@Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "is_first_login", property = "isFirstLogin"),
        @Result(column = "last_seen_at", property = "lastSeenAt")})
    User selectByUsername(@Param("username") String username);

    @Select("SELECT id, name, gender, username, birthday, bio, is_first_login, last_seen_at, create_time, update_time FROM user ORDER BY id")
    @Results({@Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "is_first_login", property = "isFirstLogin"),
        @Result(column = "last_seen_at", property = "lastSeenAt")})
    List<User> selectAll();

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectWithPassword(@Param("username") String username);

    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE user SET name = #{name}, gender = #{gender}, birthday = #{birthday}, bio = #{bio}, update_time = NOW() WHERE id = #{id}")
    int updateInfo(User user);

    @Update("UPDATE user SET is_first_login = 0 WHERE id = #{id}")
    int clearFirstLogin(@Param("id") Long id);

    @Update("UPDATE user SET last_seen_at = NOW() WHERE id = #{id}")
    int updateLastSeenAt(@Param("id") Long id);

    @Select("SELECT last_seen_at FROM user WHERE id = #{id}")
    LocalDateTime selectLastSeenAt(@Param("id") Long id);
}