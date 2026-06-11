package com.eventhub.order.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxEventMapper {

    @Insert("""
            INSERT INTO eh_outbox_event
                (event_id, aggregate_type, aggregate_id, event_type, payload)
            VALUES
                (#{eventId}, #{aggregateType}, #{aggregateId}, #{eventType}, CAST(#{payload} AS JSON))
            """)
    void insert(
            @Param("eventId") String eventId,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") long aggregateId,
            @Param("eventType") String eventType,
            @Param("payload") String payload);

    @Select("""
            SELECT id, event_id, aggregate_type, aggregate_id, event_type,
                   CAST(payload AS CHAR) AS payload, status, retry_count, next_attempt_at
            FROM eh_outbox_event
            WHERE status IN ('PENDING', 'FAILED') AND next_attempt_at <= #{now}
            ORDER BY id
            LIMIT #{limit}
            """)
    List<OutboxEventRecord> findReady(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE eh_outbox_event
            SET status = 'PUBLISHED',
                published_at = CURRENT_TIMESTAMP(3),
                last_error = NULL
            WHERE id = #{id} AND status IN ('PENDING', 'FAILED')
            """)
    int markPublished(long id);

    @Update("""
            UPDATE eh_outbox_event
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                next_attempt_at = #{nextAttemptAt},
                last_error = #{lastError}
            WHERE id = #{id} AND status IN ('PENDING', 'FAILED')
            """)
    int markFailed(
            @Param("id") long id,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastError") String lastError);
}
