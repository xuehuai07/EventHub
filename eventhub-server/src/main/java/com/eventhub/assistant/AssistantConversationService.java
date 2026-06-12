package com.eventhub.assistant;

import com.eventhub.assistant.AssistantDtos.ConversationView;
import com.eventhub.assistant.AssistantDtos.MessageView;
import com.eventhub.assistant.AssistantDtos.ResourceCard;
import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantConversationService {

    private static final TypeReference<List<ResourceCard>> RESOURCE_LIST = new TypeReference<>() {};

    private final AssistantConversationMapper mapper;
    private final ObjectMapper objectMapper;

    public AssistantConversationService(AssistantConversationMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationView create(AuthenticatedUser user, String title) {
        AssistantConversationRecord record = new AssistantConversationRecord();
        record.setUserId(user.id());
        record.setTitle(normalizeTitle(title));
        mapper.insertConversation(record);
        return view(requireOwned(user, record.getId()));
    }

    public PageResponse<ConversationView> list(AuthenticatedUser user, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 50);
        List<ConversationView> items = mapper.findConversations(user.id(), (safePage - 1) * safeSize, safeSize).stream()
                .map(this::view)
                .toList();
        return PageResponse.of(items, safePage, safeSize, mapper.countConversations(user.id()));
    }

    @Transactional
    public ConversationView rename(AuthenticatedUser user, long conversationId, String title) {
        requireOwned(user, conversationId);
        mapper.renameConversation(conversationId, user.id(), normalizeTitle(title));
        return view(requireOwned(user, conversationId));
    }

    @Transactional
    public void delete(AuthenticatedUser user, long conversationId) {
        requireOwned(user, conversationId);
        mapper.deleteConversation(conversationId, user.id());
    }

    public List<MessageView> messages(AuthenticatedUser user, long conversationId, Long beforeId, int pageSize) {
        requireOwned(user, conversationId);
        int safeSize = Math.clamp(pageSize, 1, 100);
        List<AssistantMessageRecord> records = new ArrayList<>(mapper.findMessages(conversationId, beforeId, safeSize));
        Collections.reverse(records);
        return records.stream().map(this::view).toList();
    }

    public AssistantConversationRecord requireOwned(AuthenticatedUser user, long conversationId) {
        AssistantConversationRecord conversation = mapper.findConversation(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.AI_CONVERSATION_NOT_FOUND);
        }
        if (conversation.getUserId() != user.id()) {
            throw new BusinessException(ErrorCode.AI_CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    @Transactional
    public AssistantMessageRecord saveUserMessage(long conversationId, String content) {
        AssistantMessageRecord message = new AssistantMessageRecord();
        message.setConversationId(conversationId);
        message.setRole("USER");
        message.setContent(content.trim());
        mapper.insertMessage(message);
        mapper.touchConversation(conversationId);
        return message;
    }

    @Transactional
    public AssistantMessageRecord saveAssistantMessage(
            long conversationId,
            String content,
            List<ResourceCard> resources,
            String model,
            int promptTokens,
            int completionTokens) {
        AssistantMessageRecord message = new AssistantMessageRecord();
        message.setConversationId(conversationId);
        message.setRole("ASSISTANT");
        message.setContent(content);
        message.setResourcesJson(writeResources(resources));
        message.setModel(model);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        mapper.insertMessage(message);
        mapper.touchConversation(conversationId);
        return message;
    }

    public List<AssistantMessageRecord> recentMessages(long conversationId) {
        List<AssistantMessageRecord> messages = new ArrayList<>(mapper.findRecentMessages(conversationId, 40));
        Collections.reverse(messages);
        return messages;
    }

    private ConversationView view(AssistantConversationRecord record) {
        return new ConversationView(
                record.getId(),
                record.getTitle(),
                record.getLastMessageAt(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private MessageView view(AssistantMessageRecord record) {
        return new MessageView(
                record.getId(),
                record.getRole(),
                record.getContent(),
                readResources(record.getResourcesJson()),
                record.getModel(),
                record.getPromptTokens(),
                record.getCompletionTokens(),
                record.getCreatedAt());
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        return normalized.isEmpty() ? "新的城市探索" : normalized;
    }

    private String writeResources(List<ResourceCard> resources) {
        try {
            return objectMapper.writeValueAsString(resources == null ? List.of() : resources);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize assistant resources", exception);
        }
    }

    private List<ResourceCard> readResources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, RESOURCE_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize assistant resources", exception);
        }
    }
}
