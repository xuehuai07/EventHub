package com.eventhub.activity.infrastructure.persistence;

import com.eventhub.activity.domain.ActivityStatus;
import com.eventhub.activity.dto.ActivitySummaryView;
import com.eventhub.activity.dto.CategoryView;
import com.eventhub.activity.dto.SeatAreaView;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityQueryMapper {

    String SUMMARY_SELECT = """
            SELECT activity.id, activity.title, activity.summary, activity.cover_url, activity.city,
                   category.name AS category_name, merchant.name AS merchant_name, activity.status,
                   MIN(CASE
                       WHEN session.status = 'SCHEDULED' AND session.start_at > CURRENT_TIMESTAMP(3)
                       THEN session.start_at
                   END) AS next_session_at,
                   MIN(ticket.price_cents) AS minimum_price_cents,
                   activity.version
            FROM eh_activity activity
            JOIN eh_activity_category category ON category.id = activity.category_id
            JOIN eh_merchant merchant ON merchant.id = activity.merchant_id
            LEFT JOIN eh_activity_session session ON session.activity_id = activity.id
            LEFT JOIN eh_session_ticket_type ticket ON ticket.session_id = session.id
            """;

    @Select("""
            SELECT id, code, name
            FROM eh_activity_category
            WHERE status = 'ACTIVE'
            ORDER BY sort_order, id
            """)
    List<CategoryView> findCategories();

    @Select({
        "<script>",
        SUMMARY_SELECT,
        "WHERE activity.merchant_id = #{merchantId}",
        "<if test='status != null'> AND activity.status = #{status}</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR activity.summary LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "GROUP BY activity.id, category.name, merchant.name",
        "ORDER BY activity.updated_at DESC, activity.id DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<ActivitySummaryView> findMerchantActivities(
            @Param("merchantId") long merchantId,
            @Param("status") ActivityStatus status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM eh_activity activity",
        "WHERE activity.merchant_id = #{merchantId}",
        "<if test='status != null'> AND activity.status = #{status}</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR activity.summary LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "</script>"
    })
    long countMerchantActivities(
            @Param("merchantId") long merchantId,
            @Param("status") ActivityStatus status,
            @Param("keyword") String keyword);

    @Select({
        "<script>",
        SUMMARY_SELECT,
        "WHERE activity.status = 'PUBLISHED'",
        "<if test='categoryId != null'> AND activity.category_id = #{categoryId}</if>",
        "<if test='city != null and city != \"\"'> AND activity.city = #{city}</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR activity.summary LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "<if test='date != null'> AND DATE(session.start_at) = #{date}</if>",
        "GROUP BY activity.id, category.name, merchant.name",
        "HAVING next_session_at IS NOT NULL",
        "ORDER BY next_session_at, activity.id DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<ActivitySummaryView> findPublishedActivities(
            @Param("categoryId") Long categoryId,
            @Param("city") String city,
            @Param("keyword") String keyword,
            @Param("date") LocalDate date,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT COUNT(DISTINCT activity.id)",
        "FROM eh_activity activity",
        "JOIN eh_activity_session session ON session.activity_id = activity.id",
        "WHERE activity.status = 'PUBLISHED' AND session.status = 'SCHEDULED'",
        "AND session.start_at &gt; CURRENT_TIMESTAMP(3)",
        "<if test='categoryId != null'> AND activity.category_id = #{categoryId}</if>",
        "<if test='city != null and city != \"\"'> AND activity.city = #{city}</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR activity.summary LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "<if test='date != null'> AND DATE(session.start_at) = #{date}</if>",
        "</script>"
    })
    long countPublishedActivities(
            @Param("categoryId") Long categoryId,
            @Param("city") String city,
            @Param("keyword") String keyword,
            @Param("date") LocalDate date);

    @Select("""
            SELECT activity.id, activity.merchant_id, activity.category_id,
                   category.name AS category_name, merchant.name AS merchant_name,
                   activity.title, activity.summary, activity.description, activity.cover_url,
                   activity.city, activity.status, activity.review_reason, activity.version
            FROM eh_activity activity
            JOIN eh_activity_category category ON category.id = activity.category_id
            JOIN eh_merchant merchant ON merchant.id = activity.merchant_id
            WHERE activity.id = #{activityId}
            """)
    ActivityDetailRow findDetail(long activityId);

    @Select("""
            SELECT session.id, session.activity_id, session.venue_id,
                   venue.name AS venue_name, venue.address AS venue_address, venue.seat_mode,
                   session.name, session.start_at, session.end_at,
                   session.sale_start_at, session.sale_end_at, session.status, session.version
            FROM eh_activity_session session
            JOIN eh_venue venue ON venue.id = session.venue_id
            WHERE session.activity_id = #{activityId}
            ORDER BY session.start_at, session.id
            """)
    List<SessionRecord> findSessions(long activityId);

    @Select("""
            SELECT id, session_id, name, seat_grade, price_cents, total_stock,
                   available_stock, sale_limit_per_user
            FROM eh_session_ticket_type
            WHERE session_id = #{sessionId} AND status = 'ACTIVE'
            ORDER BY price_cents, id
            """)
    List<TicketTypeRecord> findTicketTypes(long sessionId);

    @Select("""
            SELECT area_name, seat_grade, COUNT(*) AS seat_count
            FROM eh_venue_seat
            WHERE venue_id = #{venueId} AND status = 'ACTIVE'
            GROUP BY area_name, seat_grade
            ORDER BY area_name, seat_grade
            """)
    List<SeatAreaView> findSeatAreas(long venueId);

    @Select({
        "<script>",
        SUMMARY_SELECT,
        "WHERE activity.status = 'PENDING_REVIEW'",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR merchant.name LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "GROUP BY activity.id, category.name, merchant.name",
        "ORDER BY activity.updated_at, activity.id",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<ActivitySummaryView> findPendingReviews(
            @Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT COUNT(*)",
        "FROM eh_activity activity JOIN eh_merchant merchant ON merchant.id = activity.merchant_id",
        "WHERE activity.status = 'PENDING_REVIEW'",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR merchant.name LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "</script>"
    })
    long countPendingReviews(@Param("keyword") String keyword);
}
