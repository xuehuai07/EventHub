package com.eventhub.notification;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper {

    @Insert("""
            INSERT INTO eh_notification
                (user_id, type, title, content, resource_type, resource_id)
            VALUES
                (#{userId}, #{type}, #{title}, #{content}, #{resourceType}, #{resourceId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(NotificationRecord notification);

    @Select("""
            SELECT id, type, title, content, resource_type, resource_id, read_at, created_at
            FROM eh_notification
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<NotificationView> findUserNotifications(
            @Param("userId") long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM eh_notification WHERE user_id = #{userId}")
    long countUserNotifications(long userId);

    @Select("SELECT COUNT(*) FROM eh_notification WHERE user_id = #{userId} AND read_at IS NULL")
    long countUnread(long userId);

    @Update("""
            UPDATE eh_notification
            SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP(3))
            WHERE id = #{notificationId} AND user_id = #{userId}
            """)
    int markRead(@Param("notificationId") long notificationId, @Param("userId") long userId);

    @Update("""
            UPDATE eh_notification
            SET read_at = CURRENT_TIMESTAMP(3)
            WHERE user_id = #{userId} AND read_at IS NULL
            """)
    int markAllRead(long userId);
}
