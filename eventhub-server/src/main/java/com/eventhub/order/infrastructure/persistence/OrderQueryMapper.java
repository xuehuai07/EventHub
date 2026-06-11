package com.eventhub.order.infrastructure.persistence;

import com.eventhub.order.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderQueryMapper {

    @Select("""
            SELECT orders.id, orders.order_no, orders.request_id, orders.user_id, orders.merchant_id,
                   orders.activity_id, orders.session_id, orders.lock_id, orders.status,
                   orders.total_amount_cents, orders.total_quantity, orders.payment_deadline_at,
                   orders.paid_at, orders.created_at, activity.title AS activity_title,
                   session.name AS session_name, venue.name AS venue_name
            FROM eh_ticket_order orders
            JOIN eh_activity activity ON activity.id = orders.activity_id
            JOIN eh_activity_session session ON session.id = orders.session_id
            JOIN eh_venue venue ON venue.id = session.venue_id
            WHERE orders.id = #{orderId}
            """)
    OrderRecord findById(long orderId);

    @Select("""
            SELECT orders.id, orders.order_no, orders.request_id, orders.user_id, orders.merchant_id,
                   orders.activity_id, orders.session_id, orders.lock_id, orders.status,
                   orders.total_amount_cents, orders.total_quantity, orders.payment_deadline_at,
                   orders.paid_at, orders.created_at, activity.title AS activity_title,
                   session.name AS session_name, venue.name AS venue_name
            FROM eh_ticket_order orders
            JOIN eh_activity activity ON activity.id = orders.activity_id
            JOIN eh_activity_session session ON session.id = orders.session_id
            JOIN eh_venue venue ON venue.id = session.venue_id
            WHERE orders.order_no = #{orderNo}
            """)
    OrderRecord findByOrderNo(String orderNo);

    @Select("""
            SELECT orders.id, orders.order_no, orders.request_id, orders.user_id, orders.merchant_id,
                   orders.activity_id, orders.session_id, orders.lock_id, orders.status,
                   orders.total_amount_cents, orders.total_quantity, orders.payment_deadline_at,
                   orders.paid_at, orders.created_at, activity.title AS activity_title,
                   session.name AS session_name, venue.name AS venue_name
            FROM eh_ticket_order orders
            JOIN eh_activity activity ON activity.id = orders.activity_id
            JOIN eh_activity_session session ON session.id = orders.session_id
            JOIN eh_venue venue ON venue.id = session.venue_id
            WHERE orders.user_id = #{userId}
              AND (#{status} IS NULL OR orders.status = #{status})
            ORDER BY orders.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<OrderRecord> findUserOrders(
            @Param("userId") long userId,
            @Param("status") OrderStatus status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM eh_ticket_order
            WHERE user_id = #{userId} AND (#{status} IS NULL OR status = #{status})
            """)
    long countUserOrders(@Param("userId") long userId, @Param("status") OrderStatus status);

    @Select("""
            SELECT orders.id, orders.order_no, orders.request_id, orders.user_id, orders.merchant_id,
                   orders.activity_id, orders.session_id, orders.lock_id, orders.status,
                   orders.total_amount_cents, orders.total_quantity, orders.payment_deadline_at,
                   orders.paid_at, orders.created_at, activity.title AS activity_title,
                   session.name AS session_name, venue.name AS venue_name
            FROM eh_ticket_order orders
            JOIN eh_activity activity ON activity.id = orders.activity_id
            JOIN eh_activity_session session ON session.id = orders.session_id
            JOIN eh_venue venue ON venue.id = session.venue_id
            WHERE (#{merchantId} IS NULL OR orders.merchant_id = #{merchantId})
              AND (#{status} IS NULL OR orders.status = #{status})
              AND (#{keyword} IS NULL OR orders.order_no LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY orders.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<OrderRecord> findManagedOrders(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM eh_ticket_order
            WHERE (#{merchantId} IS NULL OR merchant_id = #{merchantId})
              AND (#{status} IS NULL OR status = #{status})
              AND (#{keyword} IS NULL OR order_no LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countManagedOrders(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword);

    @Select("""
            SELECT id, order_id, ticket_type_id, session_seat_id, quantity,
                   unit_price_cents, subtotal_cents, activity_title, session_name,
                   venue_name, ticket_type_name, area_name, row_label, seat_number
            FROM eh_ticket_order_item
            WHERE order_id = #{orderId}
            ORDER BY id
            """)
    List<OrderItemRecord> findItems(long orderId);

    @Select("""
            SELECT id
            FROM eh_ticket_order
            WHERE status = 'PENDING_PAYMENT' AND payment_deadline_at <= #{now}
            ORDER BY payment_deadline_at
            LIMIT #{limit}
            """)
    List<Long> findExpiredOrderIds(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
