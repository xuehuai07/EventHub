package com.eventhub.order.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AvailabilityMapper {

    @Select("""
            SELECT session.id AS session_id, activity.id AS activity_id, activity.merchant_id,
                   venue.id AS venue_id, ticket.id AS ticket_type_id,
                   activity.title AS activity_title, session.name AS session_name, venue.name AS venue_name,
                   venue.seat_mode, ticket.name AS ticket_type_name, ticket.seat_grade,
                   ticket.price_cents, ticket.total_stock, ticket.available_stock, ticket.sale_limit_per_user,
                   session.sale_start_at, session.sale_end_at, session.status AS session_status,
                   activity.status AS activity_status
            FROM eh_activity_session session
            JOIN eh_activity activity ON activity.id = session.activity_id
            JOIN eh_venue venue ON venue.id = session.venue_id
            JOIN eh_session_ticket_type ticket ON ticket.session_id = session.id AND ticket.status = 'ACTIVE'
            WHERE session.id = #{sessionId}
            ORDER BY ticket.price_cents, ticket.id
            """)
    List<SaleContextRecord> findSaleContexts(long sessionId);

    @Select("""
            SELECT session_seat.id, session_seat.session_id, session_seat.ticket_type_id,
                   venue_seat.area_name, venue_seat.row_label, venue_seat.seat_number,
                   venue_seat.seat_grade, session_seat.status, ticket.price_cents
            FROM eh_session_seat session_seat
            JOIN eh_venue_seat venue_seat ON venue_seat.id = session_seat.venue_seat_id
            LEFT JOIN eh_session_ticket_type ticket ON ticket.id = session_seat.ticket_type_id
            WHERE session_seat.session_id = #{sessionId}
            ORDER BY venue_seat.area_name, venue_seat.row_label, venue_seat.sort_order, venue_seat.id
            """)
    List<SeatRecord> findSeats(long sessionId);

    @Select({
        "<script>",
        "SELECT session_seat.id, session_seat.session_id, session_seat.ticket_type_id,",
        "venue_seat.area_name, venue_seat.row_label, venue_seat.seat_number, venue_seat.seat_grade,",
        "session_seat.status, ticket.price_cents",
        "FROM eh_session_seat session_seat",
        "JOIN eh_venue_seat venue_seat ON venue_seat.id = session_seat.venue_seat_id",
        "JOIN eh_session_ticket_type ticket ON ticket.id = session_seat.ticket_type_id",
        "WHERE session_seat.session_id = #{sessionId}",
        "AND session_seat.id IN",
        "<foreach collection='seatIds' item='seatId' open='(' separator=',' close=')'>#{seatId}</foreach>",
        "</script>"
    })
    List<SeatRecord> findSeatsByIds(@Param("sessionId") long sessionId, @Param("seatIds") List<Long> seatIds);
}
