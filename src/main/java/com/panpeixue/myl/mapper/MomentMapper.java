package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MomentMapper {

    @Insert("INSERT INTO moment (user_id, content, image, media_list, create_time) " +
            "VALUES (#{userId}, #{content}, #{image}, #{mediaListJson}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Moment moment);

    @Select("SELECT m.id, m.user_id, m.content, m.image, m.media_list, m.create_time, " +
            "u.name as user_name " +
            "FROM moment m LEFT JOIN user u ON m.user_id = u.id " +
            "WHERE m.deleted_at IS NULL " +
            "ORDER BY m.create_time DESC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "user_name", property = "userName"),
        @Result(column = "media_list", property = "mediaListJson"),
        @Result(column = "create_time", property = "createTime")
    })
    List<Moment> selectAll();

    /* 不过滤 deleted_at：删除/点赞/评论都要先查出来，再由 service 判断 404/403 */
    @Select("SELECT id, user_id, content, image, media_list, create_time, deleted_at FROM moment WHERE id = #{id}")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "media_list", property = "mediaListJson"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "deleted_at", property = "deletedAt")
    })
    Moment selectById(@Param("id") Long id);

    @Update("UPDATE moment SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id);

    /* ====== 点赞 ====== */
    @Insert("INSERT INTO moment_like (moment_id, user_id, create_time) VALUES (#{momentId}, #{userId}, NOW())")
    int insertLike(MomentLike like);

    @Delete("DELETE FROM moment_like WHERE moment_id = #{momentId} AND user_id = #{userId}")
    int deleteLike(@Param("momentId") Long momentId, @Param("userId") Long userId);

    @Select("SELECT ml.*, u.name as user_name FROM moment_like ml " +
            "LEFT JOIN user u ON ml.user_id = u.id " +
            "WHERE ml.moment_id = #{momentId} AND ml.deleted_at IS NULL " +
            "ORDER BY ml.create_time")
    @Results(@Result(column = "user_name", property = "userName"))
    List<MomentLike> selectLikesByMomentId(@Param("momentId") Long momentId);

    @Select("SELECT COUNT(*) FROM moment_like WHERE moment_id = #{momentId} AND deleted_at IS NULL")
    int countLikes(@Param("momentId") Long momentId);

    @Select("SELECT COUNT(*) FROM moment_like WHERE moment_id = #{momentId} AND user_id = #{userId} AND deleted_at IS NULL")
    int existsLike(@Param("momentId") Long momentId, @Param("userId") Long userId);

    @Update("UPDATE moment_like SET deleted_at = NOW() WHERE moment_id = #{momentId} AND deleted_at IS NULL")
    int softDeleteLikesByMomentId(@Param("momentId") Long momentId);

    /* ====== 评论 ====== */
    @Insert("INSERT INTO moment_comment (moment_id, user_id, content, create_time) VALUES (#{momentId}, #{userId}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertComment(MomentComment comment);

    @Select("SELECT mc.*, u.name as user_name FROM moment_comment mc " +
            "LEFT JOIN user u ON mc.user_id = u.id " +
            "WHERE mc.moment_id = #{momentId} AND mc.deleted_at IS NULL " +
            "ORDER BY mc.create_time")
    @Results(@Result(column = "user_name", property = "userName"))
    List<MomentComment> selectCommentsByMomentId(@Param("momentId") Long momentId);

    @Update("UPDATE moment_comment SET deleted_at = NOW() WHERE moment_id = #{momentId} AND deleted_at IS NULL")
    int softDeleteCommentsByMomentId(@Param("momentId") Long momentId);
}
