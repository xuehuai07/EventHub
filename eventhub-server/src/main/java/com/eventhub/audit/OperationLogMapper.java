package com.eventhub.audit;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationLogMapper {

    @Insert("""
            INSERT INTO eh_operation_log (
                operator_user_id, operator_name, operator_role, merchant_id,
                action, resource_type, resource_id, summary, request_id
            ) VALUES (
                #{operatorUserId}, #{operatorName}, #{operatorRole}, #{merchantId},
                #{action}, #{resourceType}, #{resourceId}, #{summary}, #{requestId}
            )
            """)
    int insert(
            @Param("operatorUserId") long operatorUserId,
            @Param("operatorName") String operatorName,
            @Param("operatorRole") String operatorRole,
            @Param("merchantId") Long merchantId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("resourceId") long resourceId,
            @Param("summary") String summary,
            @Param("requestId") String requestId);

    @Select({
        "<script>",
        "SELECT id, operator_user_id, operator_name, operator_role, merchant_id,",
        "action, resource_type, resource_id, summary, request_id, created_at",
        "FROM eh_operation_log",
        "WHERE 1 = 1",
        "<if test='operatorUserId != null'> AND operator_user_id = #{operatorUserId}</if>",
        "<if test='action != null'> AND action = #{action}</if>",
        "<if test='resourceType != null'> AND resource_type = #{resourceType}</if>",
        "<if test='startDate != null'> AND created_at &gt;= #{startDate}</if>",
        "<if test='endDate != null'> AND created_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
        "ORDER BY created_at DESC, id DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<OperationLogView> find(
            @Param("operatorUserId") Long operatorUserId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM eh_operation_log WHERE 1 = 1",
        "<if test='operatorUserId != null'> AND operator_user_id = #{operatorUserId}</if>",
        "<if test='action != null'> AND action = #{action}</if>",
        "<if test='resourceType != null'> AND resource_type = #{resourceType}</if>",
        "<if test='startDate != null'> AND created_at &gt;= #{startDate}</if>",
        "<if test='endDate != null'> AND created_at &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
        "</script>"
    })
    long count(
            @Param("operatorUserId") Long operatorUserId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
