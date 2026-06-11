package com.eventhub.order.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderCommandMapper {

    @Insert("""
            INSERT INTO eh_ticket_order
                (order_no, request_id, user_id, merchant_id, activity_id, session_id, lock_id,
                 total_amount_cents, total_quantity, payment_deadline_at)
            VALUES
                (#{orderNo}, #{requestId}, #{userId}, #{merchantId}, #{activityId}, #{sessionId}, #{lockId},
                 #{totalAmountCents}, #{totalQuantity}, #{paymentDeadlineAt})
            """)
    void insertOrder(OrderRecord order);

    @Insert({
        "<script>",
        "INSERT INTO eh_ticket_order_item",
        "(order_id, ticket_type_id, session_seat_id, quantity, unit_price_cents, subtotal_cents,",
        "activity_title, session_name, venue_name, ticket_type_name, area_name, row_label, seat_number)",
        "VALUES",
        "<foreach collection='items' item='item' separator=','>",
        "(#{item.orderId}, #{item.ticketTypeId}, #{item.sessionSeatId}, #{item.quantity},",
        "#{item.unitPriceCents}, #{item.subtotalCents}, #{item.activityTitle}, #{item.sessionName},",
        "#{item.venueName}, #{item.ticketTypeName}, #{item.areaName}, #{item.rowLabel}, #{item.seatNumber})",
        "</foreach>",
        "</script>"
    })
    void insertItems(@Param("items") List<OrderItemRecord> items);

    @Update({
        "<script>",
        "UPDATE eh_session_seat SET status = 'SOLD'",
        "WHERE status = 'AVAILABLE' AND id IN",
        "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>#{seatId}</foreach>",
        "</script>"
    })
    int sellSeats(@Param("seatIds") List<Long> seatIds);

    @Update({
        "<script>",
        "UPDATE eh_session_seat SET status = 'AVAILABLE'",
        "WHERE status = 'SOLD' AND id IN",
        "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>#{seatId}</foreach>",
        "</script>"
    })
    int restoreSeats(@Param("seatIds") List<Long> seatIds);

    @Update("""
            UPDATE eh_session_ticket_type
            SET available_stock = available_stock - #{quantity}
            WHERE id = #{ticketTypeId} AND available_stock >= #{quantity}
            """)
    int deductStock(@Param("ticketTypeId") long ticketTypeId, @Param("quantity") int quantity);

    @Update("""
            UPDATE eh_session_ticket_type
            SET available_stock = LEAST(total_stock, available_stock + #{quantity})
            WHERE id = #{ticketTypeId}
            """)
    int restoreStock(@Param("ticketTypeId") long ticketTypeId, @Param("quantity") int quantity);

    @Update("""
            UPDATE eh_ticket_order
            SET status = #{targetStatus},
                paid_at = CASE WHEN #{targetStatus} = 'PAID' THEN CURRENT_TIMESTAMP(3) ELSE paid_at END,
                cancelled_at = CASE WHEN #{targetStatus} = 'CANCELLED' THEN CURRENT_TIMESTAMP(3) ELSE cancelled_at END,
                expired_at = CASE WHEN #{targetStatus} = 'EXPIRED' THEN CURRENT_TIMESTAMP(3) ELSE expired_at END,
                version = version + 1
            WHERE id = #{orderId} AND status = 'PENDING_PAYMENT'
            """)
    int transition(@Param("orderId") long orderId, @Param("targetStatus") String targetStatus);

    @Update("""
            UPDATE eh_ticket_order
            SET status = 'EXPIRED',
                expired_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{orderId}
              AND status = 'PENDING_PAYMENT'
              AND payment_deadline_at <= #{now}
            """)
    int expire(@Param("orderId") long orderId, @Param("now") LocalDateTime now);

    @Insert("""
            INSERT INTO eh_payment_record (payment_no, order_id, amount_cents, status, paid_at)
            VALUES (#{paymentNo}, #{orderId}, #{amountCents}, 'SUCCESS', CURRENT_TIMESTAMP(3))
            """)
    void insertPayment(
            @Param("paymentNo") String paymentNo,
            @Param("orderId") long orderId,
            @Param("amountCents") long amountCents);

    @Insert("""
            INSERT INTO eh_idempotency_record
                (user_id, scope, idempotency_key, request_hash, resource_type, resource_id, expires_at)
            VALUES
                (#{userId}, #{scope}, #{key}, #{requestHash}, #{resourceType}, #{resourceId}, #{expiresAt})
            """)
    void insertIdempotency(
            @Param("userId") long userId,
            @Param("scope") String scope,
            @Param("key") String key,
            @Param("requestHash") String requestHash,
            @Param("resourceType") String resourceType,
            @Param("resourceId") long resourceId,
            @Param("expiresAt") LocalDateTime expiresAt);

    @Select("""
            SELECT request_hash
            FROM eh_idempotency_record
            WHERE user_id = #{userId} AND scope = #{scope} AND idempotency_key = #{key}
            """)
    String findIdempotencyHash(@Param("userId") long userId, @Param("scope") String scope, @Param("key") String key);

    @Select("""
            SELECT resource_id
            FROM eh_idempotency_record
            WHERE user_id = #{userId} AND scope = #{scope} AND idempotency_key = #{key}
            """)
    Long findIdempotencyResource(@Param("userId") long userId, @Param("scope") String scope, @Param("key") String key);
}
