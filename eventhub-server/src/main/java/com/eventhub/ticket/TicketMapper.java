package com.eventhub.ticket;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketMapper {

    @Insert("""
            INSERT INTO eh_ticket
                (ticket_no, order_id, order_item_id, unit_no, user_id, activity_id, session_id)
            VALUES
                (#{ticketNo}, #{orderId}, #{orderItemId}, #{unitNo}, #{userId}, #{activityId}, #{sessionId})
            """)
    void insert(
            @Param("ticketNo") String ticketNo,
            @Param("orderId") long orderId,
            @Param("orderItemId") long orderItemId,
            @Param("unitNo") int unitNo,
            @Param("userId") long userId,
            @Param("activityId") long activityId,
            @Param("sessionId") long sessionId);

    @Select("SELECT COUNT(*) FROM eh_ticket WHERE order_id = #{orderId}")
    int countByOrderId(long orderId);

    @Select(TICKET_SELECT + " WHERE ticket.id = #{ticketId}")
    TicketRecord findById(long ticketId);

    @Select(TICKET_SELECT + " WHERE ticket.ticket_no = #{ticketNo}")
    TicketRecord findByTicketNo(String ticketNo);

    @Select(TICKET_SELECT + """
             WHERE ticket.user_id = #{userId}
               AND (#{status} IS NULL OR ticket.status = #{status})
             ORDER BY session.start_at, ticket.id
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<TicketRecord> findUserTickets(
            @Param("userId") long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM eh_ticket
            WHERE user_id = #{userId} AND (#{status} IS NULL OR status = #{status})
            """)
    long countUserTickets(@Param("userId") long userId, @Param("status") String status);

    @Select(TICKET_SELECT + """
             WHERE ticket.order_id = #{orderId} AND ticket.user_id = #{userId}
             ORDER BY ticket.id
            """)
    List<TicketRecord> findOrderTickets(@Param("orderId") long orderId, @Param("userId") long userId);

    @Update("""
            UPDATE eh_ticket ticket
            JOIN eh_ticket_order orders ON orders.id = ticket.order_id
            SET ticket.status = 'USED',
                ticket.used_at = #{usedAt},
                ticket.verified_by = #{operatorId},
                ticket.verification_device = #{deviceId}
            WHERE ticket.id = #{ticketId}
              AND orders.merchant_id = #{merchantId}
              AND ticket.status = 'UNUSED'
            """)
    int markUsed(
            @Param("ticketId") long ticketId,
            @Param("merchantId") long merchantId,
            @Param("operatorId") long operatorId,
            @Param("deviceId") String deviceId,
            @Param("usedAt") LocalDateTime usedAt);

    @Insert("""
            INSERT INTO eh_ticket_verification_log
                (ticket_id, merchant_id, operator_id, result, device_id, request_ip, verified_at)
            VALUES
                (#{ticketId}, #{merchantId}, #{operatorId}, #{result}, #{deviceId}, #{requestIp}, #{verifiedAt})
            """)
    void insertVerificationLog(
            @Param("ticketId") long ticketId,
            @Param("merchantId") long merchantId,
            @Param("operatorId") long operatorId,
            @Param("result") String result,
            @Param("deviceId") String deviceId,
            @Param("requestIp") String requestIp,
            @Param("verifiedAt") LocalDateTime verifiedAt);

    @Select("""
            SELECT log.id, log.result, log.device_id, log.request_ip, log.verified_at,
                   ticket.ticket_no, item.activity_title, item.session_name, item.venue_name,
                   user.display_name AS operator_name
            FROM eh_ticket_verification_log log
            JOIN eh_ticket ticket ON ticket.id = log.ticket_id
            JOIN eh_ticket_order_item item ON item.id = ticket.order_item_id
            JOIN eh_user user ON user.id = log.operator_id
            WHERE (#{merchantId} IS NULL OR log.merchant_id = #{merchantId})
            ORDER BY log.verified_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<VerificationLogView> findVerificationLogs(
            @Param("merchantId") Long merchantId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM eh_ticket_verification_log
            WHERE (#{merchantId} IS NULL OR merchant_id = #{merchantId})
            """)
    long countVerificationLogs(@Param("merchantId") Long merchantId);

    String TICKET_SELECT = """
            SELECT ticket.id, ticket.ticket_no, ticket.order_id, ticket.user_id, ticket.status,
                   ticket.used_at, ticket.verified_by, ticket.verification_device,
                   orders.order_no, orders.merchant_id,
                   item.activity_title, item.session_name, item.venue_name, item.ticket_type_name,
                   item.area_name, item.row_label, item.seat_number,
                   session.start_at, activity.cover_url, user.display_name AS user_display_name,
                   verifier.display_name AS verifier_name
            FROM eh_ticket ticket
            JOIN eh_ticket_order orders ON orders.id = ticket.order_id
            JOIN eh_ticket_order_item item ON item.id = ticket.order_item_id
            JOIN eh_activity_session session ON session.id = ticket.session_id
            JOIN eh_activity activity ON activity.id = ticket.activity_id
            JOIN eh_user user ON user.id = ticket.user_id
            LEFT JOIN eh_user verifier ON verifier.id = ticket.verified_by
            """;
}
