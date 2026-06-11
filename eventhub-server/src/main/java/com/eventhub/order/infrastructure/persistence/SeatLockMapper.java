package com.eventhub.order.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeatLockMapper {

    @Insert("""
            INSERT INTO eh_seat_lock_record
                (lock_no, user_id, session_id, ticket_type_id, seat_mode, quantity, expires_at)
            VALUES
                (#{lockNo}, #{userId}, #{sessionId}, #{ticketTypeId}, #{seatMode}, #{quantity}, #{expiresAt})
            """)
    void insert(SeatLockRecord record);

    @Insert({
        "<script>",
        "INSERT INTO eh_seat_lock_item (lock_id, session_seat_id) VALUES",
        "<foreach collection='seatIds' item='seatId' separator=','>",
        "(#{lockId}, #{seatId})",
        "</foreach>",
        "</script>"
    })
    void insertItems(@Param("lockId") long lockId, @Param("seatIds") List<Long> seatIds);

    @Select("""
            SELECT id, lock_no, user_id, session_id, ticket_type_id, seat_mode,
                   quantity, status, consumed_order_id, expires_at
            FROM eh_seat_lock_record
            WHERE lock_no = #{lockNo}
            """)
    SeatLockRecord findByLockNo(String lockNo);

    @Select("""
            SELECT id, lock_no, user_id, session_id, ticket_type_id, seat_mode,
                   quantity, status, consumed_order_id, expires_at
            FROM eh_seat_lock_record
            WHERE id = #{lockId}
            """)
    SeatLockRecord findById(long lockId);

    @Select("SELECT session_seat_id FROM eh_seat_lock_item WHERE lock_id = #{lockId} ORDER BY id")
    List<Long> findSeatIds(long lockId);

    @Update("""
            UPDATE eh_seat_lock_record
            SET status = #{targetStatus}, consumed_order_id = #{orderId}
            WHERE id = #{lockId} AND status = 'ACTIVE'
            """)
    int updateStatus(
            @Param("lockId") long lockId, @Param("targetStatus") String targetStatus, @Param("orderId") Long orderId);

    @Update("""
            UPDATE eh_seat_lock_record
            SET status = 'EXPIRED'
            WHERE status = 'ACTIVE' AND expires_at <= #{now}
            """)
    int expireLocks(LocalDateTime now);
}
