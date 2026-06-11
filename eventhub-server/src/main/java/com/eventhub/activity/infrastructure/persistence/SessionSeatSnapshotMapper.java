package com.eventhub.activity.infrastructure.persistence;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionSeatSnapshotMapper {

    @Select("SELECT seat_mode FROM eh_venue WHERE id = #{venueId}")
    String findSeatMode(long venueId);

    @Delete("DELETE FROM eh_session_seat WHERE session_id = #{sessionId}")
    void deleteBySession(long sessionId);

    @Insert("""
            INSERT INTO eh_session_seat (session_id, venue_seat_id, ticket_type_id, status)
            SELECT #{sessionId}, seat.id, ticket.id,
                   CASE WHEN ticket.id IS NULL THEN 'DISABLED' ELSE 'AVAILABLE' END
            FROM eh_venue_seat seat
            LEFT JOIN eh_session_ticket_type ticket
                   ON ticket.session_id = #{sessionId}
                  AND ticket.status = 'ACTIVE'
                  AND ticket.seat_grade = seat.seat_grade
            WHERE seat.venue_id = #{venueId} AND seat.status = 'ACTIVE'
            """)
    void insertFixedSnapshot(@Param("sessionId") long sessionId, @Param("venueId") long venueId);

    @Select("""
            SELECT COUNT(*)
            FROM eh_session_seat
            WHERE session_id = #{sessionId} AND ticket_type_id IS NULL
            """)
    int countUnmappedSeats(long sessionId);

    @Update("""
            UPDATE eh_session_ticket_type ticket
            LEFT JOIN (
                SELECT ticket_type_id, COUNT(*) AS seat_count
                FROM eh_session_seat
                WHERE session_id = #{sessionId} AND ticket_type_id IS NOT NULL
                GROUP BY ticket_type_id
            ) snapshot ON snapshot.ticket_type_id = ticket.id
            SET ticket.total_stock = COALESCE(snapshot.seat_count, 0),
                ticket.available_stock = COALESCE(snapshot.seat_count, 0)
            WHERE ticket.session_id = #{sessionId}
            """)
    void synchronizeFixedStock(long sessionId);

    @Select("""
            SELECT COUNT(*)
            FROM eh_activity_session session
            JOIN eh_venue venue ON venue.id = session.venue_id AND venue.seat_mode = 'FIXED'
            JOIN eh_session_seat seat ON seat.session_id = session.id
            WHERE session.activity_id = #{activityId} AND seat.ticket_type_id IS NULL
            """)
    int countActivityUnmappedSeats(long activityId);
}
