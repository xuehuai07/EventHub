package com.eventhub.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AssistantDtos {

    private AssistantDtos() {}

    public record ConversationView(
            long id, String title, LocalDateTime lastMessageAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record MessageView(
            long id,
            String role,
            String content,
            List<ResourceCard> resources,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            LocalDateTime createdAt) {}

    public record ConversationRequest(
            @NotBlank @Size(max = 100) String title) {}

    public record MessageRequest(@NotBlank @Size(max = 2000) String content) {}

    public record ResourceCard(String type, long id, String title, String subtitle, String href) {}

    public record AckEvent(long conversationId, long userMessageId) {}

    public record DeltaEvent(String content) {}

    public record ResourcesEvent(List<ResourceCard> items) {}

    public record DoneEvent(long assistantMessageId, String model, int promptTokens, int completionTokens) {}

    public record ErrorEvent(String code, String message, boolean retryable) {}
}
