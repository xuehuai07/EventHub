package com.eventhub.assistant;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AssistantProperties properties;

    public DeepSeekClient(HttpClient assistantHttpClient, ObjectMapper objectMapper, AssistantProperties properties) {
        this.httpClient = assistantHttpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public DeepSeekTurn complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        return complete(messages, tools, timeout());
    }

    public DeepSeekTurn complete(
            List<Map<String, Object>> messages, List<Map<String, Object>> tools, Duration requestTimeout) {
        return complete(messages, tools, requestTimeout, ignored -> {});
    }

    public DeepSeekTurn complete(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools,
            Duration requestTimeout,
            Consumer<String> contentConsumer) {
        if (!properties.configured()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.model());
        payload.put("messages", messages);
        payload.put("tools", tools);
        payload.put("tool_choice", "auto");
        payload.put("stream", true);
        payload.put("stream_options", Map.of("include_usage", true));
        payload.put("thinking", Map.of("type", "disabled"));

        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(write(payload), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (BufferedReader body =
                        new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    body.lines().limit(20).forEach(ignored -> {});
                }
                throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE);
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                return parse(reader, contentConsumer);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 请求已取消");
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE);
        }
    }

    DeepSeekTurn parse(BufferedReader reader) throws IOException {
        return parse(reader, ignored -> {});
    }

    private DeepSeekTurn parse(BufferedReader reader, Consumer<String> contentConsumer) throws IOException {
        List<String> chunks = new ArrayList<>();
        Map<Integer, ToolCallBuilder> toolCalls = new LinkedHashMap<>();
        int promptTokens = 0;
        int completionTokens = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(data);
            } catch (JsonProcessingException exception) {
                throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 流式响应格式错误");
            }
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode() && !usage.isNull()) {
                promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                completionTokens = usage.path("completion_tokens").asInt(completionTokens);
            }
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                continue;
            }
            JsonNode delta = choices.get(0).path("delta");
            JsonNode content = delta.get("content");
            if (content != null && content.isTextual() && !content.textValue().isEmpty()) {
                chunks.add(content.textValue());
                contentConsumer.accept(content.textValue());
            }
            JsonNode calls = delta.path("tool_calls");
            if (calls.isArray()) {
                for (JsonNode call : calls) {
                    int index = call.path("index").asInt();
                    ToolCallBuilder builder = toolCalls.computeIfAbsent(index, ignored -> new ToolCallBuilder());
                    append(builder.id, call.path("id").asText(""));
                    JsonNode function = call.path("function");
                    append(builder.name, function.path("name").asText(""));
                    append(builder.arguments, function.path("arguments").asText(""));
                }
            }
        }
        List<ToolCall> calls = toolCalls.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue().build())
                .toList();
        return new DeepSeekTurn(chunks, calls, promptTokens, completionTokens);
    }

    private URI endpoint() {
        String base = properties.baseUrl().toString().replaceAll("/+$", "");
        return URI.create(base + "/chat/completions");
    }

    private Duration timeout() {
        return properties.timeout() == null ? Duration.ofSeconds(60) : properties.timeout();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize DeepSeek request", exception);
        }
    }

    private void append(StringBuilder target, String value) {
        if (value != null && !value.isEmpty()) {
            target.append(value);
        }
    }

    private static final class ToolCallBuilder {

        private final StringBuilder id = new StringBuilder();
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();

        ToolCall build() {
            return new ToolCall(id.toString(), name.toString(), arguments.toString());
        }
    }

    public record DeepSeekTurn(List<String> chunks, List<ToolCall> toolCalls, int promptTokens, int completionTokens) {

        public String content() {
            return String.join("", chunks);
        }
    }

    public record ToolCall(String id, String name, String arguments) {}
}
