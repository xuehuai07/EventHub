package com.eventhub.ticket;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
