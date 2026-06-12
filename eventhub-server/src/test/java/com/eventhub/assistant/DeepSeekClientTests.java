package com.eventhub.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventhub.common.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeepSeekClientTests {

    private final DeepSeekClient client = new DeepSeekClient(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            new AssistantProperties(
                    "test", URI.create("http://127.0.0.1"), "deepseek-v4-flash", Duration.ofSeconds(5)));

    @Test
    void parsesTextToolCallFragmentsAndUsage() throws Exception {
        String stream = """
                data: {"choices":[{"delta":{"content":"先查"}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_","function":{"name":"search_","arguments":"{\\"city\\":"}}]}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"1","function":{"name":"activities","arguments":"\\"上海\\"}"}}]}}]}

                data: {"choices":[],"usage":{"prompt_tokens":21,"completion_tokens":8}}

                data: [DONE]

                """;

        DeepSeekClient.DeepSeekTurn turn = client.parse(new BufferedReader(new StringReader(stream)));

        assertThat(turn.content()).isEqualTo("先查");
        assertThat(turn.promptTokens()).isEqualTo(21);
        assertThat(turn.completionTokens()).isEqualTo(8);
        assertThat(turn.toolCalls()).singleElement().satisfies(call -> {
            assertThat(call.id()).isEqualTo("call_1");
            assertThat(call.name()).isEqualTo("search_activities");
            assertThat(call.arguments()).isEqualTo("{\"city\":\"上海\"}");
        });
    }

    @Test
    void consumesToolCallsFromLocalStreamingService() throws Exception {
        HttpServer server = startServer(200, """
                data: {"choices":[{"delta":{"content":"正在查"}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"name":"list_my_tickets","arguments":"{\\"status\\":\\"UNUSED\\"}"}}]}}]}

                data: {"choices":[],"usage":{"prompt_tokens":10,"completion_tokens":5}}

                data: [DONE]

                """);
        try {
            DeepSeekClient localClient = clientFor(server);
            List<String> streamed = new ArrayList<>();

            DeepSeekClient.DeepSeekTurn turn = localClient.complete(
                    List.of(Map.of("role", "user", "content", "查票")), List.of(), Duration.ofSeconds(5), streamed::add);

            assertThat(streamed).containsExactly("正在查");
            assertThat(turn.toolCalls()).singleElement().satisfies(call -> {
                assertThat(call.name()).isEqualTo("list_my_tickets");
                assertThat(call.arguments()).isEqualTo("{\"status\":\"UNUSED\"}");
            });
            assertThat(turn.promptTokens()).isEqualTo(10);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsLocalUpstreamFailureToBusinessError() throws Exception {
        HttpServer server = startServer(503, "{\"error\":\"unavailable\"}");
        try {
            DeepSeekClient localClient = clientFor(server);

            assertThatThrownBy(
                            () -> localClient.complete(List.of(Map.of("role", "user", "content", "推荐活动")), List.of()))
                    .isInstanceOf(BusinessException.class);
        } finally {
            server.stop(0);
        }
    }

    private DeepSeekClient clientFor(HttpServer server) {
        return new DeepSeekClient(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                new AssistantProperties(
                        "test",
                        URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                        "deepseek-v4-flash",
                        Duration.ofSeconds(5)));
    }

    private HttpServer startServer(int status, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", status == 200 ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        return server;
    }
}
