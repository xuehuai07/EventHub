package com.eventhub.review;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityReviewMapper {

    String VIEW_SELECT = """
            SELECT review.id, review.activity_id, activity.title AS activity_title,
                   review.user_id, user.display_name AS user_display_name,
                   review.rating, review.content, review.status, review.hidden_reason,
                   review.created_at, review.updated_at
            FROM eh_activity_review review
            JOIN eh_activity activity ON activity.id = review.activity_id
            JOIN eh_user user ON user.id = review.user_id
            """;

    @Select("""
            SELECT COUNT(*)
            FROM eh_ticket_order ticket_order
            JOIN eh_activity_session session ON session.id = ticket_order.session_id
            WHERE ticket_order.user_id = #{userId}
              AND ticket_order.activity_id = #{activityId}
              AND ticket_order.status = 'PAID'
              AND session.start_at <= CURRENT_TIMESTAMP(3)
            """)
    int countEligibleOrder(@Param("userId") long userId, @Param("activityId") long activityId);

    @Select("""
            SELECT COUNT(*) FROM eh_activity
            WHERE id = #{activityId} AND status = 'PUBLISHED'
            """)
    int countPublishedActivity(long activityId);

    @Insert("""
            INSERT INTO eh_activity_review (user_id, activity_id, rating, content)
            VALUES (#{userId}, #{activityId}, #{rating}, #{content})
            """)
    int insert(
            @Param("userId") long userId,
            @Param("activityId") long activityId,
            @Param("rating") int rating,
            @Param("content") String content);

    @Update("""
            UPDATE eh_activity_review
            SET rating = #{rating}, content = #{content}
            WHERE user_id = #{userId} AND activity_id = #{activityId}
            """)
    int updateMine(
            @Param("userId") long userId,
            @Param("activityId") long activityId,
            @Param("rating") int rating,
            @Param("content") String content);

    @Delete("DELETE FROM eh_activity_review WHERE user_id = #{userId} AND activity_id = #{activityId}")
    int deleteMine(@Param("userId") long userId, @Param("activityId") long activityId);

    @Select(VIEW_SELECT + " WHERE review.user_id = #{userId} AND review.activity_id = #{activityId}")
    ActivityReviewView findMine(@Param("userId") long userId, @Param("activityId") long activityId);

    @Select(VIEW_SELECT + """
             WHERE review.activity_id = #{activityId} AND review.status = 'PUBLISHED'
             ORDER BY review.created_at DESC, review.id DESC
             LIMIT #{limit} OFFSET #{offset}
             """)
    List<ActivityReviewView> findPublic(
            @Param("activityId") long activityId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*) FROM eh_activity_review
            WHERE activity_id = #{activityId} AND status = 'PUBLISHED'
            """)
    long countPublic(long activityId);

    @Select("""
            SELECT ROUND(AVG(rating), 1) FROM eh_activity_review
            WHERE activity_id = #{activityId} AND status = 'PUBLISHED'
            """)
    Double averageRating(long activityId);

    @Select(VIEW_SELECT + """
             WHERE (#{status} IS NULL OR review.status = #{status})
               AND (#{keyword} IS NULL OR activity.title LIKE CONCAT('%', #{keyword}, '%')
                    OR user.display_name LIKE CONCAT('%', #{keyword}, '%')
                    OR review.content LIKE CONCAT('%', #{keyword}, '%'))
             ORDER BY review.created_at DESC, review.id DESC
             LIMIT #{limit} OFFSET #{offset}
             """)
    List<ActivityReviewView> findAdmin(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM eh_activity_review review
            JOIN eh_activity activity ON activity.id = review.activity_id
            JOIN eh_user user ON user.id = review.user_id
            WHERE (#{status} IS NULL OR review.status = #{status})
              AND (#{keyword} IS NULL OR activity.title LIKE CONCAT('%', #{keyword}, '%')
                   OR user.display_name LIKE CONCAT('%', #{keyword}, '%')
                   OR review.content LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countAdmin(@Param("status") String status, @Param("keyword") String keyword);

    @Select(VIEW_SELECT + " WHERE review.id = #{reviewId}")
    ActivityReviewView findById(long reviewId);

    @Update("""
            UPDATE eh_activity_review
            SET status = 'HIDDEN', hidden_reason = #{reason}, moderator_id = #{moderatorId},
                moderated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{reviewId} AND status = 'PUBLISHED'
            """)
    int hide(@Param("reviewId") long reviewId, @Param("moderatorId") long moderatorId, @Param("reason") String reason);

    @Update("""
            UPDATE eh_activity_review
            SET status = 'PUBLISHED', hidden_reason = NULL, moderator_id = #{moderatorId},
                moderated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{reviewId} AND status = 'HIDDEN'
            """)
    int restore(@Param("reviewId") long reviewId, @Param("moderatorId") long moderatorId);
}
