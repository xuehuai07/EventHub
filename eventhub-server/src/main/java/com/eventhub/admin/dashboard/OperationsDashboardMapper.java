package com.eventhub.admin.dashboard;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationsDashboardMapper {

    @Select({
        "<script>",
        "SELECT",
        "(SELECT COALESCE(SUM(total_amount_cents), 0) FROM eh_ticket_order",
        " WHERE status = 'PAID' <if test='merchantId != null'>AND merchant_id = #{merchantId}</if>) AS paid_amount_cents,",
        "(SELECT COUNT(*) FROM eh_ticket_order",
        " WHERE status = 'PAID' <if test='merchantId != null'>AND merchant_id = #{merchantId}</if>) AS paid_order_count,",
        "(SELECT COALESCE(SUM(total_quantity), 0) FROM eh_ticket_order",
        " WHERE status = 'PAID' <if test='merchantId != null'>AND merchant_id = #{merchantId}</if>) AS sold_ticket_count,",
        "(SELECT COUNT(*) FROM eh_ticket ticket JOIN eh_ticket_order ticket_order ON ticket_order.id = ticket.order_id",
        " WHERE ticket.status = 'USED' <if test='merchantId != null'>AND ticket_order.merchant_id = #{merchantId}</if>) AS used_ticket_count,",
        "(SELECT COUNT(*) FROM eh_activity WHERE status = 'PUBLISHED'",
        " <if test='merchantId != null'>AND merchant_id = #{merchantId}</if>) AS published_activity_count,",
        "<choose><when test='merchantId == null'>",
        "(SELECT COUNT(*) FROM eh_merchant WHERE status = 'ACTIVE')",
        "</when><otherwise>1</otherwise></choose> AS active_merchant_count",
        "</script>"
    })
    OperationsDashboardView operations(@Param("merchantId") Long merchantId);

    @Select({
        "<script>",
        "SELECT DATE(paid_at) AS date, SUM(total_amount_cents) AS paid_amount_cents,",
        "COUNT(*) AS paid_order_count, SUM(total_quantity) AS sold_ticket_count",
        "FROM eh_ticket_order",
        "WHERE status = 'PAID' AND paid_at &gt;= #{startDate}",
        "AND paid_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)",
        "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if>",
        "GROUP BY DATE(paid_at) ORDER BY date",
        "</script>"
    })
    List<SalesTrendView> salesTrend(
            @Param("merchantId") Long merchantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select({
        "<script>",
        "SELECT ticket_order.activity_id, activity.title,",
        "SUM(ticket_order.total_amount_cents) AS paid_amount_cents,",
        "SUM(ticket_order.total_quantity) AS sold_ticket_count",
        "FROM eh_ticket_order ticket_order",
        "JOIN eh_activity activity ON activity.id = ticket_order.activity_id",
        "WHERE ticket_order.status = 'PAID'",
        "<if test='merchantId != null'>AND ticket_order.merchant_id = #{merchantId}</if>",
        "GROUP BY ticket_order.activity_id, activity.title",
        "ORDER BY paid_amount_cents DESC, sold_ticket_count DESC, ticket_order.activity_id",
        "LIMIT #{limit}",
        "</script>"
    })
    List<TopActivityView> topActivities(@Param("merchantId") Long merchantId, @Param("limit") int limit);
}
