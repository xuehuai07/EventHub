package com.eventhub.assistant;

import com.eventhub.assistant.AssistantDtos.AckEvent;
import com.eventhub.assistant.AssistantDtos.DeltaEvent;
import com.eventhub.assistant.AssistantDtos.DoneEvent;
import com.eventhub.assistant.AssistantDtos.ResourceCard;
import com.eventhub.assistant.AssistantDtos.ResourcesEvent;
import com.eventhub.assistant.AssistantStreamLock.LockHandle;
import com.eventhub.assistant.AssistantToolExecutor.ToolResult;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AssistantStreamingService {

    private static final int MAX_TOOL_ROUNDS = 3;

    private final AssistantProperties properties;
    private final AssistantConversationService conversations;
    private final AssistantToolExecutor tools;
    private final DeepSeekClient deepSeek;
    private final AssistantStreamLock streamLock;
    private final AssistantSseEvents events;
    private final ExecutorService assistantExecutor;
    private final String systemPrompt;

    public AssistantStreamingService(
            AssistantProperties properties,
            AssistantConversationService conversations,
            AssistantToolExecutor tools,
            DeepSeekClient deepSeek,
            AssistantStreamLock streamLock,
            AssistantSseEvents events,
            ExecutorService assistantExecutor) {
        this.properties = properties;
        this.conversations = conversations;
        this.tools = tools;
        this.deepSeek = deepSeek;
        this.streamLock = streamLock;
        this.events = events;
        this.assistantExecutor = assistantExecutor;
        this.systemPrompt = loadPrompt();
    }

    public SseEmitter stream(AuthenticatedUser user, long conversationId, String content) {
        if (!properties.configured()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
        conversations.requireOwned(user, conversationId);
        LockHandle lock = streamLock.acquire(user.id());
        if (lock == null) {
            throw new BusinessException(ErrorCode.AI_STREAM_BUSY);
        }

        AssistantMessageRecord userMessage;
        try {
            userMessage = conversations.saveUserMessage(conversationId, content);
        } catch (RuntimeException exception) {
            streamLock.release(lock);
            throw exception;
        }

        SseEmitter emitter = events.create();
        AtomicBoolean terminal = new AtomicBoolean();
        events.send(emitter, "ack", new AckEvent(conversationId, userMessage.getId()));
        Future<?> future;
        try {
            future = assistantExecutor.submit(() -> generate(user, conversationId, emitter, terminal, lock));
        } catch (RejectedExecutionException exception) {
            streamLock.release(lock);
            throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 服务繁忙，请稍后重试");
        }
        emitter.onCompletion(() -> {
            terminal.set(true);
            future.cancel(true);
            streamLock.release(lock);
        });
        emitter.onTimeout(() -> {
            terminal.set(true);
            future.cancel(true);
            streamLock.release(lock);
        });
        emitter.onError(ignored -> {
            terminal.set(true);
            future.cancel(true);
            streamLock.release(lock);
        });
        return emitter;
    }

    private void generate(
            AuthenticatedUser user, long conversationId, SseEmitter emitter, AtomicBoolean terminal, LockHandle lock) {
        try {
            List<Map<String, Object>> messages = buildHistory(conversationId);
            List<ResourceCard> resources = new ArrayList<>();
            StringBuilder visibleContent = new StringBuilder();
            int promptTokens = 0;
            int completionTokens = 0;
            Duration totalTimeout = properties.timeout() == null ? Duration.ofSeconds(60) : properties.timeout();
            long deadline = System.nanoTime() + totalTimeout.toNanos();

            for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
                Duration remaining = Duration.ofNanos(deadline - System.nanoTime());
                if (remaining.isNegative() || remaining.isZero()) {
                    throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 请求超时，请重试");
                }
                DeepSeekClient.DeepSeekTurn turn =
                        deepSeek.complete(messages, tools.definitions(), remaining, chunk -> {
                            visibleContent.append(chunk);
                            if (!terminal.get()) {
                                events.send(emitter, "delta", new DeltaEvent(chunk));
                            }
                        });
                promptTokens += turn.promptTokens();
                completionTokens += turn.completionTokens();
                if (turn.toolCalls().isEmpty()) {
                    String answer = visibleContent.toString().trim();
                    if (turn.content().isBlank()) {
                        String fallback = "暂时没有生成有效回答，请稍后重试。";
                        events.send(emitter, "delta", new DeltaEvent(answer.isEmpty() ? fallback : "\n" + fallback));
                        answer = answer.isEmpty() ? fallback : answer + "\n" + fallback;
                    }
                    List<ResourceCard> distinctResources =
                            resources.stream().distinct().toList();
                    if (!distinctResources.isEmpty()) {
                        events.send(emitter, "resources", new ResourcesEvent(distinctResources));
                    }
                    if (terminal.get()) {
                        return;
                    }
                    AssistantMessageRecord saved = conversations.saveAssistantMessage(
                            conversationId,
                            answer,
                            distinctResources,
                            properties.model(),
                            promptTokens,
                            completionTokens);
                    events.send(
                            emitter,
                            "done",
                            new DoneEvent(saved.getId(), properties.model(), promptTokens, completionTokens));
                    terminal.set(true);
                    emitter.complete();
                    return;
                }
                if (round == MAX_TOOL_ROUNDS) {
                    throw new BusinessException(ErrorCode.AI_TOOL_LIMIT_EXCEEDED);
                }
                messages.add(assistantToolMessage(turn));
                for (DeepSeekClient.ToolCall call : turn.toolCalls()) {
                    ToolResult result = tools.execute(call.name(), call.arguments(), user);
                    resources.addAll(result.resources());
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", call.id(),
                            "name", call.name(),
                            "content", result.json()));
                }
            }
        } catch (BusinessException exception) {
            events.error(emitter, terminal, exception.getErrorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            events.error(emitter, terminal, ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 服务暂时不可用");
        } finally {
            streamLock.release(lock);
        }
    }

    private List<Map<String, Object>> buildHistory(long conversationId) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (AssistantMessageRecord message : conversations.recentMessages(conversationId)) {
            messages.add(Map.of(
                    "role", message.getRole().toLowerCase(),
                    "content", message.getContent()));
        }
        return messages;
    }

    private Map<String, Object> assistantToolMessage(DeepSeekClient.DeepSeekTurn turn) {
        List<Map<String, Object>> calls = turn.toolCalls().stream()
                .map(call -> Map.<String, Object>of(
                        "id",
                        call.id(),
                        "type",
                        "function",
                        "function",
                        Map.of("name", call.name(), "arguments", call.arguments())))
                .toList();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", turn.content());
        message.put("tool_calls", calls);
        return message;
    }

    private String loadPrompt() {
        try {
            return new ClassPathResource("prompts/assistant-system.md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load assistant system prompt", exception);
        }
    }
}
