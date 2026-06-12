package com.eventhub.activity.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityCommandMapper {

    @Select("""
            SELECT id, merchant_id, category_id, title, summary, description, cover_url,
                   city, status, review_reason, version
            FROM eh_activity
            WHERE id = #{activityId}
            """)
    ActivityRecord findActivity(long activityId);

    @Insert("""
            INSERT INTO eh_activity
                (merchant_id, category_id, title, summary, description, cover_url, city)
            VALUES
                (#{merchantId}, #{categoryId}, #{title}, #{summary}, #{description}, #{coverUrl}, #{city})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertActivity(ActivityRecord activity);

    @Update("""
            UPDATE eh_activity
            SET category_id = #{categoryId},
                title = #{title},
                summary = #{summary},
                description = #{description},
                cover_url = #{coverUrl},
                city = #{city},
                review_reason = NULL,
                version = version + 1
            WHERE id = #{id} AND merchant_id = #{merchantId} AND version = #{version}
            """)
    int updateActivity(ActivityRecord activity);

    @Update("""
            UPDATE eh_activity
            SET summary = #{summary},
                description = #{description},
                cover_url = #{coverUrl},
                version = version + 1
            WHERE id = #{id}
              AND merchant_id = #{merchantId}
              AND status = 'PUBLISHED'
              AND version = #{version}
            """)
    int updatePublishedContent(ActivityRecord activity);

    @Insert("""
            INSERT INTO eh_activity_session
                (activity_id, venue_id, name, start_at, end_at, sale_start_at, sale_end_at)
            VALUES
                (#{activityId}, #{venueId}, #{name}, #{startAt}, #{endAt}, #{saleStartAt}, #{saleEndAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSession(SessionRecord session);

    @Update("""
            UPDATE eh_activity_session
            SET venue_id = #{venueId},
                name = #{name},
                start_at = #{startAt},
                end_at = #{endAt},
                sale_start_at = #{saleStartAt},
                sale_end_at = #{saleEndAt},
                version = version + 1
            WHERE id = #{id} AND activity_id = #{activityId} AND version = #{version}
            """)
    int updateSession(SessionRecord session);

    @Select("""
            SELECT session.id, session.activity_id, session.venue_id,
                   venue.name AS venue_name, venue.address AS venue_address, venue.seat_mode,
                   session.name, session.start_at, session.end_at,
                   session.sale_start_at, session.sale_end_at, session.status, session.version
            FROM eh_activity_session session
            JOIN eh_venue venue ON venue.id = session.venue_id
            WHERE session.id = #{sessionId} AND session.activity_id = #{activityId}
            """)
    SessionRecord findSession(@Param("activityId") long activityId, @Param("sessionId") long sessionId);

    @Delete("DELETE FROM eh_session_ticket_type WHERE session_id = #{sessionId}")
    void deleteTicketTypes(long sessionId);

    @Delete("DELETE FROM eh_session_seat WHERE session_id = #{sessionId}")
    void deleteSessionSeats(long sessionId);

    @Insert({
        "<script>",
        "INSERT INTO eh_session_ticket_type",
        "(session_id, name, seat_grade, price_cents, total_stock, available_stock, sale_limit_per_user)",
        "VALUES",
        "<foreach collection='tickets' item='ticket' separator=','>",
        "(#{ticket.sessionId}, #{ticket.name}, #{ticket.seatGrade}, #{ticket.priceCents},",
        "#{ticket.totalStock}, #{ticket.availableStock}, #{ticket.saleLimitPerUser})",
        "</foreach>",
        "</script>"
    })
    void insertTicketTypes(@Param("tickets") List<TicketTypeRecord> tickets);

    @Delete("DELETE FROM eh_activity_session WHERE id = #{sessionId} AND activity_id = #{activityId}")
    int deleteSession(@Param("activityId") long activityId, @Param("sessionId") long sessionId);

    @Select("""
            SELECT COUNT(*)
            FROM eh_activity_session session
            WHERE session.activity_id = #{activityId}
              AND EXISTS (
                  SELECT 1 FROM eh_session_ticket_type ticket WHERE ticket.session_id = session.id
              )
            """)
    int countCompleteSessions(long activityId);

    @Update("""
            UPDATE eh_activity
            SET status = 'PENDING_REVIEW',
                review_reason = NULL,
                reviewer_id = NULL,
                reviewed_at = NULL,
                version = version + 1
            WHERE id = #{activityId} AND merchant_id = #{merchantId}
              AND status IN ('DRAFT', 'REJECTED')
            """)
    int submit(@Param("activityId") long activityId, @Param("merchantId") long merchantId);

    @Update("""
            UPDATE eh_activity
            SET status = 'PUBLISHED',
                review_reason = NULL,
                reviewer_id = #{reviewerId},
                reviewed_at = CURRENT_TIMESTAMP(3),
                published_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{activityId} AND status = 'PENDING_REVIEW'
            """)
    int approve(@Param("activityId") long activityId, @Param("reviewerId") long reviewerId);

    @Update("""
            UPDATE eh_activity
            SET status = 'REJECTED',
                review_reason = #{reason},
                reviewer_id = #{reviewerId},
                reviewed_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{activityId} AND status = 'PENDING_REVIEW'
            """)
    int reject(
            @Param("activityId") long activityId, @Param("reviewerId") long reviewerId, @Param("reason") String reason);

    @Update("""
            UPDATE eh_activity
            SET status = 'OFF_SHELF',
                review_reason = #{reason},
                reviewer_id = #{reviewerId},
                reviewed_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{activityId} AND status = 'PUBLISHED'
            """)
    int offShelf(
            @Param("activityId") long activityId, @Param("reviewerId") long reviewerId, @Param("reason") String reason);
}
