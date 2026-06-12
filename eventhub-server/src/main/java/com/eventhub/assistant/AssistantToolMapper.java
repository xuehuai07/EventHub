package com.eventhub.assistant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssistantToolMapper {

    @Select({
        "<script>",
        """
        SELECT activity.id, activity.title, activity.summary, activity.city,
               category.name AS category_name, MIN(session.start_at) AS next_session_at,
               MIN(ticket.price_cents) AS minimum_price_cents
        FROM eh_activity activity
        JOIN eh_activity_category category ON category.id = activity.category_id
        JOIN eh_activity_session session ON session.activity_id = activity.id
        JOIN eh_session_ticket_type ticket ON ticket.session_id = session.id AND ticket.status = 'ACTIVE'
        WHERE activity.status = 'PUBLISHED'
          AND session.status = 'SCHEDULED'
          AND session.start_at > CURRENT_TIMESTAMP(3)
          AND session.sale_end_at > CURRENT_TIMESTAMP(3)
        """,
        "<if test='keyword != null'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR activity.summary LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "<if test='city != null'> AND activity.city = #{city}</if>",
        "<if test='category != null'>",
        "AND (category.code = #{category} OR category.name LIKE CONCAT('%', #{category}, '%'))",
        "</if>",
        "<if test='date != null'> AND DATE(session.start_at) = #{date}</if>",
        "GROUP BY activity.id, activity.title, activity.summary, activity.city, category.name",
        "<if test='maxPriceCents != null'> HAVING MIN(ticket.price_cents) &lt;= #{maxPriceCents}</if>",
        "ORDER BY next_session_at, activity.id DESC",
        "LIMIT #{limit}",
        "</script>"
    })
    List<ActivityToolRow> searchActivities(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("category") String category,
            @Param("date") LocalDate date,
            @Param("maxPriceCents") Long maxPriceCents,
            @Param("limit") int limit);

    @Select("""
            SELECT session.id, activity.id AS activity_id, activity.title AS activity_title,
                   session.name, session.start_at, session.end_at, venue.name AS venue_name,
                   venue.address AS venue_address, venue.seat_mode,
                   MIN(ticket.price_cents) AS minimum_price_cents,
                   MAX(ticket.price_cents) AS maximum_price_cents,
                   SUM(ticket.available_stock) AS available_stock
            FROM eh_activity_session session
            JOIN eh_activity activity ON activity.id = session.activity_id
            JOIN eh_venue venue ON venue.id = session.venue_id
            JOIN eh_session_ticket_type ticket ON ticket.session_id = session.id AND ticket.status = 'ACTIVE'
            WHERE activity.id = #{activityId}
              AND activity.status = 'PUBLISHED'
              AND session.status = 'SCHEDULED'
              AND session.start_at > CURRENT_TIMESTAMP(3)
              AND session.sale_start_at <= CURRENT_TIMESTAMP(3)
              AND session.sale_end_at > CURRENT_TIMESTAMP(3)
            GROUP BY session.id, activity.id, activity.title, session.name, session.start_at,
                     session.end_at, venue.name, venue.address, venue.seat_mode
            ORDER BY session.start_at, session.id
            LIMIT 10
            """)
    List<SessionToolRow> findActivitySessions(long activityId);

    @Select({
        "<script>",
        """
        SELECT orders.id, activity.title AS activity_title, session.name AS session_name,
               venue.name AS venue_name, orders.total_amount_cents, orders.total_quantity,
               orders.paid_at
        FROM eh_ticket_order orders
        JOIN eh_activity activity ON activity.id = orders.activity_id
        JOIN eh_activity_session session ON session.id = orders.session_id
        JOIN eh_venue venue ON venue.id = session.venue_id
        WHERE orders.user_id = #{userId} AND orders.status = 'PAID'
        """,
        "<if test='keyword != null'>",
        "AND (activity.title LIKE CONCAT('%', #{keyword}, '%')",
        "OR session.name LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "ORDER BY orders.paid_at DESC, orders.id DESC",
        "LIMIT #{limit}",
        "</script>"
    })
    List<OrderToolRow> findPaidOrders(
            @Param("userId") long userId, @Param("keyword") String keyword, @Param("limit") int limit);

    @Select("""
            SELECT id
            FROM eh_ticket_order
            WHERE user_id = #{userId}
              AND status = 'PAID'
              AND (id = #{orderId} OR order_no = #{orderReference})
            LIMIT 1
            """)
    Long findOwnedPaidOrder(
            @Param("userId") long userId,
            @Param("orderId") Long orderId,
            @Param("orderReference") String orderReference);

    @Select("""
            SELECT ticket.id, ticket.order_id, ticket.status, ticket.used_at,
                   item.activity_title, item.session_name, item.venue_name,
                   item.ticket_type_name, item.area_name, item.row_label, item.seat_number,
                   session.start_at
            FROM eh_ticket ticket
            JOIN eh_ticket_order_item item ON item.id = ticket.order_item_id
            JOIN eh_activity_session session ON session.id = ticket.session_id
            WHERE ticket.user_id = #{userId} AND ticket.order_id = #{orderId}
            ORDER BY ticket.id
            LIMIT 20
            """)
    List<TicketToolRow> findOrderTickets(@Param("userId") long userId, @Param("orderId") long orderId);

    @Select({
        "<script>",
        """
        SELECT ticket.id, ticket.order_id, ticket.status, ticket.used_at,
               item.activity_title, item.session_name, item.venue_name,
               item.ticket_type_name, item.area_name, item.row_label, item.seat_number,
               session.start_at
        FROM eh_ticket ticket
        JOIN eh_ticket_order_item item ON item.id = ticket.order_item_id
        JOIN eh_activity_session session ON session.id = ticket.session_id
        WHERE ticket.user_id = #{userId}
        """,
        "<if test='status != null'> AND ticket.status = #{status}</if>",
        "ORDER BY session.start_at, ticket.id",
        "LIMIT #{limit}",
        "</script>"
    })
    List<TicketToolRow> findTickets(
            @Param("userId") long userId, @Param("status") String status, @Param("limit") int limit);

    record ActivityToolRow(
            long id,
            String title,
            String summary,
            String city,
            String categoryName,
            LocalDateTime nextSessionAt,
            long minimumPriceCents) {}

    record SessionToolRow(
            long id,
            long activityId,
            String activityTitle,
            String name,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String venueName,
            String venueAddress,
            String seatMode,
            long minimumPriceCents,
            long maximumPriceCents,
            long availableStock) {}

    record OrderToolRow(
            long id,
            String activityTitle,
            String sessionName,
            String venueName,
            long totalAmountCents,
            int totalQuantity,
            LocalDateTime paidAt) {}

    record TicketToolRow(
            long id,
            long orderId,
            String status,
            LocalDateTime usedAt,
            String activityTitle,
            String sessionName,
            String venueName,
            String ticketTypeName,
            String areaName,
            String rowLabel,
            String seatNumber,
            LocalDateTime startAt) {}
}
