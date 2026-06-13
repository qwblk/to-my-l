package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MomentMapper {

    @Insert("INSERT INTO moment (user_id, content, image, create_time) VALUES (#{userId}, #{content}, #{image}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Moment moment);

    @Select("SELECT m.*, u.name as user_name FROM moment m LEFT JOIN user u ON m.user_id = u.id ORDER BY m.create_time DESC")
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "user_name", property = "userName")
    })
    List<Moment> selectAll();

    @Select("SELECT * FROM moment WHERE id = #{id}")
    Moment selectById(@Param("id") Long id);

    /* ====== 点赞 ====== */
    @Insert("INSERT INTO moment_like (moment_id, user_id, create_time) VALUES (#{momentId}, #{userId}, NOW())")
    int insertLike(MomentLike like);

    @Delete("DELETE FROM moment_like WHERE moment_id = #{momentId} AND user_id = #{userId}")
    int deleteLike(@Param("momentId") Long momentId, @Param("userId") Long userId);

    @Select("SELECT ml.*, u.name as user_name FROM moment_like ml LEFT JOIN user u ON ml.user_id = u.id WHERE ml.moment_id = #{momentId} ORDER BY ml.create_time")
    @Results(@Result(column = "user_name", property = "userName"))
    List<MomentLike> selectLikesByMomentId(@Param("momentId") Long momentId);

    @Select("SELECT COUNT(*) FROM moment_like WHERE moment_id = #{momentId}")
    int countLikes(@Param("momentId") Long momentId);

    @Select("SELECT COUNT(*) FROM moment_like WHERE moment_id = #{momentId} AND user_id = #{userId}")
    int existsLike(@Param("momentId") Long momentId, @Param("userId") Long userId);

    /* ====== 评论 ====== */
    @Insert("INSERT INTO moment_comment (moment_id, user_id, content, create_time) VALUES (#{momentId}, #{userId}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertComment(MomentComment comment);

    @Select("SELECT mc.*, u.name as user_name FROM moment_comment mc LEFT JOIN user u ON mc.user_id = u.id WHERE mc.moment_id = #{momentId} ORDER BY mc.create_time")
    @Results(@Result(column = "user_name", property = "userName"))
    List<MomentComment> selectCommentsByMomentId(@Param("momentId") Long momentId);
}