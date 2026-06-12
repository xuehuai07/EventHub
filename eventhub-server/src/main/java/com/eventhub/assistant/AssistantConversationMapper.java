package com.eventhub.assistant;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AssistantConversationMapper {

    @Insert("""
            INSERT INTO eh_ai_conversation (user_id, title)
            VALUES (#{userId}, #{title})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertConversation(AssistantConversationRecord conversation);

    @Select("""
            SELECT id, user_id, title, last_message_at, created_at, updated_at
            FROM eh_ai_conversation
            WHERE id = #{conversationId}
            """)
    AssistantConversationRecord findConversation(long conversationId);

    @Select("""
            SELECT id, user_id, title, last_message_at, created_at, updated_at
            FROM eh_ai_conversation
            WHERE user_id = #{userId}
            ORDER BY last_message_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AssistantConversationRecord> findConversations(
            @Param("userId") long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM eh_ai_conversation WHERE user_id = #{userId}")
    long countConversations(long userId);

    @Update("""
            UPDATE eh_ai_conversation
            SET title = #{title}
            WHERE id = #{conversationId} AND user_id = #{userId}
            """)
    int renameConversation(
            @Param("conversationId") long conversationId, @Param("userId") long userId, @Param("title") String title);

    @Update("""
            UPDATE eh_ai_conversation
            SET last_message_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{conversationId}
            """)
    void touchConversation(long conversationId);

    @Delete("DELETE FROM eh_ai_conversation WHERE id = #{conversationId} AND user_id = #{userId}")
    int deleteConversation(@Param("conversationId") long conversationId, @Param("userId") long userId);

    @Insert("""
            INSERT INTO eh_ai_message
                (conversation_id, role, content, resources_json, model, prompt_tokens, completion_tokens)
            VALUES
                (#{conversationId}, #{role}, #{content}, CAST(#{resourcesJson} AS JSON),
                 #{model}, #{promptTokens}, #{completionTokens})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(AssistantMessageRecord message);

    @Select("""
            SELECT id, conversation_id, role, content, resources_json, model,
                   prompt_tokens, completion_tokens, created_at
            FROM eh_ai_message
            WHERE conversation_id = #{conversationId}
              AND (#{beforeId} IS NULL OR id < #{beforeId})
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AssistantMessageRecord> findMessages(
            @Param("conversationId") long conversationId, @Param("beforeId") Long beforeId, @Param("limit") int limit);

    @Select("""
            SELECT id, conversation_id, role, content, resources_json, model,
                   prompt_tokens, completion_tokens, created_at
            FROM eh_ai_message
            WHERE conversation_id = #{conversationId}
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AssistantMessageRecord> findRecentMessages(
            @Param("conversationId") long conversationId, @Param("limit") int limit);
}
