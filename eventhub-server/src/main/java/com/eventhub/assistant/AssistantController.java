package com.eventhub.assistant;

import com.eventhub.assistant.AssistantDtos.ConversationRequest;
import com.eventhub.assistant.AssistantDtos.ConversationView;
import com.eventhub.assistant.AssistantDtos.MessageRequest;
import com.eventhub.assistant.AssistantDtos.MessageView;
import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/assistant/conversations")
@PreAuthorize("hasRole('USER')")
public class AssistantController {

    private final AssistantConversationService conversations;
    private final AssistantStreamingService streaming;

    public AssistantController(AssistantConversationService conversations, AssistantStreamingService streaming) {
        this.conversations = conversations;
        this.streaming = streaming;
    }

    @GetMapping
    @Operation(operationId = "listAssistantConversations", summary = "查询本人 AI 会话")
    ApiResponse<PageResponse<ConversationView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(conversations.list(user, page, pageSize));
    }

    @PostMapping
    @Operation(operationId = "createAssistantConversation", summary = "新建 AI 会话")
    ApiResponse<ConversationView> create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ConversationRequest request) {
        return ApiResponse.success(conversations.create(user, request.title()));
    }

    @PutMapping("/{conversationId}")
    @Operation(operationId = "renameAssistantConversation", summary = "重命名本人 AI 会话")
    ApiResponse<ConversationView> rename(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long conversationId,
            @Valid @RequestBody ConversationRequest request) {
        return ApiResponse.success(conversations.rename(user, conversationId, request.title()));
    }

    @DeleteMapping("/{conversationId}")
    @Operation(operationId = "deleteAssistantConversation", summary = "删除本人 AI 会话")
    ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long conversationId) {
        conversations.delete(user, conversationId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(operationId = "listAssistantMessages", summary = "查询本人 AI 会话消息")
    ApiResponse<List<MessageView>> messages(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.success(conversations.messages(user, conversationId, beforeId, pageSize));
    }

    @PostMapping(path = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(hidden = true)
    SseEmitter stream(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long conversationId,
            @Valid @RequestBody MessageRequest request) {
        return streaming.stream(user, conversationId, request.content());
    }
}
