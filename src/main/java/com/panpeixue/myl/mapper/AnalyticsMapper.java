package com.panpeixue.myl.mapper;

import com.panpeixue.myl.model.dto.AnalyticsCountRow;
import com.panpeixue.myl.model.dto.AnalyticsDailyRawCount;
import com.panpeixue.myl.model.pojo.AnalyticsEvent;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnalyticsMapper {

    String filterWhere = "<where>"
        + " create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)"
        + " <choose>"
        + "   <when test='anonymous'>AND user_id IS NULL</when>"
        + "   <when test='userId != null'>AND user_id = #{userId}</when>"
        + " </choose>"
        + "</where>";

    @Insert({
        "<script>",
        "INSERT INTO analytics_event (user_id, visitor_id, event_type, path, detail, ip, user_agent, create_time)",
        "VALUES (#{userId}, #{visitorId}, #{eventType}, #{path},",
        "<choose>",
        "  <when test='detail != null'>#{detail}</when>",
        "  <otherwise>NULL</otherwise>",
        "</choose>",
        ", #{ip}, #{userAgent}, NOW())",
        "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalyticsEvent event);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM analytics_event",
        filterWhere,
        "</script>"
    })
    long countTotal(@Param("days") int days,
                    @Param("userId") Long userId,
                    @Param("anonymous") boolean anonymous);

    @Select({
        "<script>",
        "SELECT COUNT(DISTINCT visitor_id) FROM analytics_event",
        filterWhere,
        "AND visitor_id IS NOT NULL",
        "</script>"
    })
    long countUniqueVisitors(@Param("days") int days,
                             @Param("userId") Long userId,
                             @Param("anonymous") boolean anonymous);

    @Select({
        "<script>",
        "SELECT MAX(create_time) FROM analytics_event",
        filterWhere,
        "</script>"
    })
    LocalDateTime selectLastVisitAt(@Param("days") int days,
                                    @Param("userId") Long userId,
                                    @Param("anonymous") boolean anonymous);

    @Select({
        "<script>",
        "SELECT event_type, COUNT(*) AS count FROM analytics_event",
        filterWhere,
        "GROUP BY event_type",
        "</script>"
    })
    @Results({
        @Result(column = "event_type", property = "eventType"),
        @Result(column = "count", property = "count")
    })
    List<AnalyticsCountRow> countByEventType(@Param("days") int days,
                                             @Param("userId") Long userId,
                                             @Param("anonymous") boolean anonymous);

    @Select({
        "<script>",
        "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, event_type, COUNT(*) AS count",
        "FROM analytics_event",
        filterWhere,
        "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d'), event_type ORDER BY date ASC",
        "</script>"
    })
    @Results({
        @Result(column = "date", property = "date"),
        @Result(column = "event_type", property = "eventType"),
        @Result(column = "count", property = "count")
    })
    List<AnalyticsDailyRawCount> countDaily(@Param("days") int days,
                                            @Param("userId") Long userId,
                                            @Param("anonymous") boolean anonymous);

    @Select({
        "<script>",
        "SELECT * FROM analytics_event",
        filterWhere,
        "ORDER BY create_time DESC, id DESC LIMIT #{limit}",
        "</script>"
    })
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "visitor_id", property = "visitorId"),
        @Result(column = "event_type", property = "eventType"),
        @Result(column = "user_agent", property = "userAgent"),
        @Result(column = "create_time", property = "createTime")
    })
    List<AnalyticsEvent> selectRecentByDays(@Param("days") int days,
                                            @Param("userId") Long userId,
                                            @Param("anonymous") boolean anonymous,
                                            @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT * FROM analytics_event",
        "<where>",
        "  <choose>",
        "    <when test='anonymous'>user_id IS NULL</when>",
        "    <when test='userId != null'>user_id = #{userId}</when>",
        "  </choose>",
        "</where>",
        "ORDER BY create_time DESC, id DESC LIMIT #{limit}",
        "</script>"
    })
    @Results({
        @Result(column = "user_id", property = "userId"),
        @Result(column = "visitor_id", property = "visitorId"),
        @Result(column = "event_type", property = "eventType"),
        @Result(column = "user_agent", property = "userAgent"),
        @Result(column = "create_time", property = "createTime")
    })
    List<AnalyticsEvent> selectRecent(@Param("userId") Long userId,
                                      @Param("anonymous") boolean anonymous,
                                      @Param("limit") int limit);
}
