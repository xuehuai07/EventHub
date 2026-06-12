package com.eventhub.favorite;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityFavoriteMapper {

    @Select("SELECT COUNT(*) FROM eh_activity WHERE id = #{activityId} AND status = 'PUBLISHED'")
    int countPublishedActivity(long activityId);

    @Insert("""
            INSERT IGNORE INTO eh_activity_favorite (user_id, activity_id)
            VALUES (#{userId}, #{activityId})
            """)
    int insert(@Param("userId") long userId, @Param("activityId") long activityId);

    @Delete("DELETE FROM eh_activity_favorite WHERE user_id = #{userId} AND activity_id = #{activityId}")
    int delete(@Param("userId") long userId, @Param("activityId") long activityId);

    @Select("""
            SELECT COUNT(*) FROM eh_activity_favorite
            WHERE user_id = #{userId} AND activity_id = #{activityId}
            """)
    int countFavorite(@Param("userId") long userId, @Param("activityId") long activityId);

    @Select("""
            SELECT activity.id AS activity_id, activity.title, activity.summary, activity.cover_url,
                   activity.city, category.name AS category_name, activity.status,
                   MIN(CASE WHEN session.status = 'SCHEDULED' AND session.start_at > CURRENT_TIMESTAMP(3)
                       THEN session.start_at END) AS next_session_at,
                   MIN(ticket.price_cents) AS minimum_price_cents,
                   favorite.created_at AS favorited_at
            FROM eh_activity_favorite favorite
            JOIN eh_activity activity ON activity.id = favorite.activity_id
            JOIN eh_activity_category category ON category.id = activity.category_id
            LEFT JOIN eh_activity_session session ON session.activity_id = activity.id
            LEFT JOIN eh_session_ticket_type ticket ON ticket.session_id = session.id AND ticket.status = 'ACTIVE'
            WHERE favorite.user_id = #{userId}
            GROUP BY favorite.id, activity.id, category.name
            ORDER BY favorite.created_at DESC, favorite.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ActivityFavoriteView> findByUser(
            @Param("userId") long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM eh_activity_favorite WHERE user_id = #{userId}")
    long countByUser(long userId);
}
