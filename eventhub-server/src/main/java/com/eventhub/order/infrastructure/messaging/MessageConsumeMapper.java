package com.eventhub.order.infrastructure.messaging;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageConsumeMapper {

    @Insert("""
            INSERT IGNORE INTO eh_message_consume_record (consumer_name, event_id)
            VALUES (#{consumerName}, #{eventId})
            """)
    int tryRecord(@Param("consumerName") String consumerName, @Param("eventId") String eventId);
}
