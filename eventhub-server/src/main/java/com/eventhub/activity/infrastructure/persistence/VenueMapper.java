package com.eventhub.activity.infrastructure.persistence;

import com.eventhub.activity.dto.SeatAreaView;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VenueMapper {

    @Select("""
            SELECT id, merchant_id, name, city, address, seat_mode, capacity, status, version
            FROM eh_venue
            WHERE merchant_id = #{merchantId}
            ORDER BY updated_at DESC, id DESC
            """)
    List<VenueRecord> findByMerchant(long merchantId);

    @Select("""
            SELECT id, merchant_id, name, city, address, seat_mode, capacity, status, version
            FROM eh_venue
            WHERE id = #{venueId}
            """)
    VenueRecord findById(long venueId);

    @Insert("""
            INSERT INTO eh_venue (merchant_id, name, city, address, seat_mode, capacity)
            VALUES (#{merchantId}, #{name}, #{city}, #{address}, #{seatMode}, #{capacity})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(VenueRecord venue);

    @Update("""
            UPDATE eh_venue
            SET name = #{name},
                city = #{city},
                address = #{address},
                seat_mode = #{seatMode},
                capacity = #{capacity},
                version = version + 1
            WHERE id = #{id} AND merchant_id = #{merchantId} AND version = #{version}
            """)
    int update(VenueRecord venue);

    @Select("""
            SELECT area_name, seat_grade, COUNT(*) AS seat_count
            FROM eh_venue_seat
            WHERE venue_id = #{venueId} AND status = 'ACTIVE'
            GROUP BY area_name, seat_grade
            ORDER BY area_name, seat_grade
            """)
    List<SeatAreaView> findSeatAreas(long venueId);

    @Delete("DELETE FROM eh_venue_seat WHERE venue_id = #{venueId}")
    void deleteSeats(long venueId);

    @Insert({
        "<script>",
        "INSERT INTO eh_venue_seat",
        "(venue_id, area_name, row_label, seat_number, seat_code, seat_grade, sort_order)",
        "VALUES",
        "<foreach collection='seats' item='seat' separator=','>",
        "(#{seat.venueId}, #{seat.areaName}, #{seat.rowLabel}, #{seat.seatNumber},",
        "#{seat.seatCode}, #{seat.seatGrade}, #{seat.sortOrder})",
        "</foreach>",
        "</script>"
    })
    void insertSeats(@Param("seats") List<VenueSeatRecord> seats);

    @Update("UPDATE eh_venue SET capacity = #{capacity}, version = version + 1 WHERE id = #{venueId}")
    void updateCapacity(@Param("venueId") long venueId, @Param("capacity") int capacity);
}
