package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.Diary;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DiaryMapper {

    @Insert("INSERT INTO diary (user_id, title, content, mood, weather, is_private, create_time) " +
            "VALUES (#{userId}, #{title}, #{content}, #{mood}, #{weather}, #{isPrivate}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Diary diary);

    @Select("SELECT d.*, u.name as user_name FROM diary d LEFT JOIN user u ON d.user_id = u.id " +
            "WHERE d.deleted_at IS NULL " +
            "ORDER BY d.create_time DESC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "is_private", property = "isPrivate"),
        @Result(column = "user_name", property = "userName"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    List<Diary> selectAll();

    @Select("SELECT d.*, u.name as user_name FROM diary d LEFT JOIN user u ON d.user_id = u.id " +
            "WHERE d.user_id = #{userId} AND d.deleted_at IS NULL ORDER BY d.create_time DESC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "is_private", property = "isPrivate"),
        @Result(column = "user_name", property = "userName"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    List<Diary> selectByUserId(@Param("userId") Long userId);

    /* 不过滤 deleted_at：delete 业务需要判断不存在/已删/无权限 */
    @Select("SELECT d.*, u.name as user_name FROM diary d LEFT JOIN user u ON d.user_id = u.id WHERE d.id = #{id}")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "is_private", property = "isPrivate"),
        @Result(column = "user_name", property = "userName"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    Diary selectById(@Param("id") Long id);

    @Update("UPDATE diary SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE diary SET is_private = #{isPrivate} WHERE id = #{id} AND deleted_at IS NULL")
    int updatePrivacy(@Param("id") Long id, @Param("isPrivate") Integer isPrivate);

    /**
     * scope=all 可见日期：自己的全部 + 对方非私密，且未软删。
     * cursorDate 语义：只取 date < cursorDate，避免翻页重复上一页最后一天。
     */
    @Select({
        "<script>",
        "SELECT DISTINCT DATE(create_time) FROM diary",
        "WHERE deleted_at IS NULL",
        "AND (user_id = #{currentUserId} OR is_private = 0)",
        "<if test='cursorDate != null'>AND DATE(create_time) &lt; #{cursorDate}</if>",
        "ORDER BY DATE(create_time) DESC",
        "LIMIT #{limit}",
        "</script>"
    })
    List<LocalDate> selectVisibleDatesAll(@Param("currentUserId") Long currentUserId,
                                          @Param("cursorDate") LocalDate cursorDate,
                                          @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT DISTINCT DATE(create_time) FROM diary",
        "WHERE deleted_at IS NULL",
        "AND user_id = #{currentUserId}",
        "<if test='cursorDate != null'>AND DATE(create_time) &lt; #{cursorDate}</if>",
        "ORDER BY DATE(create_time) DESC",
        "LIMIT #{limit}",
        "</script>"
    })
    List<LocalDate> selectVisibleDatesMine(@Param("currentUserId") Long currentUserId,
                                           @Param("cursorDate") LocalDate cursorDate,
                                           @Param("limit") int limit);

    @Select("SELECT d.*, u.name as user_name FROM diary d LEFT JOIN user u ON d.user_id = u.id " +
            "WHERE d.deleted_at IS NULL " +
            "AND (d.user_id = #{currentUserId} OR d.is_private = 0) " +
            "AND DATE(d.create_time) = #{date} " +
            "ORDER BY d.create_time ASC, d.id ASC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "is_private", property = "isPrivate"),
        @Result(column = "user_name", property = "userName"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    List<Diary> selectVisibleEntriesByDateAll(@Param("currentUserId") Long currentUserId,
                                              @Param("date") LocalDate date);

    @Select("SELECT d.*, u.name as user_name FROM diary d LEFT JOIN user u ON d.user_id = u.id " +
            "WHERE d.deleted_at IS NULL " +
            "AND d.user_id = #{currentUserId} " +
            "AND DATE(d.create_time) = #{date} " +
            "ORDER BY d.create_time ASC, d.id ASC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "is_private", property = "isPrivate"),
        @Result(column = "user_name", property = "userName"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    List<Diary> selectVisibleEntriesByDateMine(@Param("currentUserId") Long currentUserId,
                                               @Param("date") LocalDate date);
}
