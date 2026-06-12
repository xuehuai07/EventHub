package com.eventhub.assistant;

import com.eventhub.assistant.AssistantDtos.ResourceCard;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class AssistantToolExecutor {

    private final AssistantToolMapper mapper;
    private final ObjectMapper objectMapper;

    public AssistantToolExecutor(AssistantToolMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> definitions() {
        return List.of(
                definition(
                        "search_activities",
                        "搜索 EventHub 中真实、已发布且有未来可售场次的活动，最多返回 5 个。",
                        properties(
                                property("keyword", "string", "活动关键词"),
                                property("city", "string", "城市"),
                                property("category", "string", "活动分类名称或编码"),
                                property("date", "string", "日期，格式 YYYY-MM-DD"),
                                property("maxPriceCents", "integer", "最高预算，单位为分"),
                                property("limit", "integer", "返回数量，最大 5"))),
                definition(
                        "get_activity_sessions",
                        "查询指定活动未来正在售票的场次和库存。",
                        requiredProperties(List.of("activityId"), property("activityId", "integer", "活动 ID"))),
                definition(
                        "list_my_paid_orders",
                        "查询当前登录用户本人最近的已支付订单。",
                        properties(
                                property("keyword", "string", "活动或场次关键词"), property("limit", "integer", "返回数量，最大 10"))),
                definition(
                        "get_my_order_tickets",
                        "查询当前登录用户本人某个已支付订单下的票券。",
                        properties(
                                property("orderId", "integer", "订单 ID"), property("orderNo", "string", "用户提供的完整订单号"))),
                definition(
                        "list_my_tickets",
                        "查询当前登录用户本人的票券和使用状态。",
                        properties(
                                property("status", "string", "UNUSED、USED 或 CANCELLED"),
                                property("limit", "integer", "返回数量，最大 20"))));
    }

    public ToolResult execute(String name, String arguments, AuthenticatedUser user) {
        JsonNode args = readArguments(arguments);
        return switch (name) {
            case "search_activities" -> searchActivities(args);
            case "get_activity_sessions" -> activitySessions(args);
            case "list_my_paid_orders" -> paidOrders(args, user);
            case "get_my_order_tickets" -> orderTickets(args, user);
            case "list_my_tickets" -> tickets(args, user);
            default -> throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 请求了未注册的工具");
        };
    }

    private ToolResult searchActivities(JsonNode args) {
        LocalDate date = dateValue(args, "date");
        int limit = Math.clamp(intValue(args, "limit", 5), 1, 5);
        List<AssistantToolMapper.ActivityToolRow> rows = mapper.searchActivities(
                text(args, "keyword"),
                text(args, "city"),
                text(args, "category"),
                date,
                longValue(args, "maxPriceCents"),
                limit);
        List<ResourceCard> resources = rows.stream()
                .map(row -> new ResourceCard(
                        "ACTIVITY",
                        row.id(),
                        row.title(),
                        row.city() + " · " + row.categoryName(),
                        "/activities/" + row.id()))
                .toList();
        return result(rows, resources);
    }

    private ToolResult activitySessions(JsonNode args) {
        long activityId = requiredLong(args, "activityId");
        List<AssistantToolMapper.SessionToolRow> rows = mapper.findActivitySessions(activityId);
        List<ResourceCard> resources = rows.stream()
                .map(row -> new ResourceCard(
                        "SESSION",
                        row.id(),
                        row.activityTitle() + " · " + row.name(),
                        row.venueName() + " · " + row.startAt(),
                        "/sessions/" + row.id() + "/tickets"))
                .toList();
        return result(rows, resources);
    }

    private ToolResult paidOrders(JsonNode args, AuthenticatedUser user) {
        int limit = Math.clamp(intValue(args, "limit", 10), 1, 10);
        List<AssistantToolMapper.OrderToolRow> rows = mapper.findPaidOrders(user.id(), text(args, "keyword"), limit);
        List<ResourceCard> resources = rows.stream()
                .map(row -> new ResourceCard(
                        "ORDER",
                        row.id(),
                        row.activityTitle(),
                        "已支付 · " + row.sessionName() + " · " + row.venueName(),
                        "/orders/" + row.id()))
                .toList();
        return result(rows, resources);
    }

    private ToolResult orderTickets(JsonNode args, AuthenticatedUser user) {
        Long orderId = longValue(args, "orderId");
        String orderNo = text(args, "orderNo");
        if (orderId == null && orderNo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "需要订单 ID 或订单号");
        }
        Long ownedOrderId = mapper.findOwnedPaidOrder(user.id(), orderId, orderNo);
        if (ownedOrderId == null) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        List<AssistantToolMapper.TicketToolRow> rows = mapper.findOrderTickets(user.id(), ownedOrderId);
        return ticketResult(rows);
    }

    private ToolResult tickets(JsonNode args, AuthenticatedUser user) {
        String status = text(args, "status");
        if (status != null && !List.of("UNUSED", "USED", "CANCELLED").contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "票券状态不正确");
        }
        int limit = Math.clamp(intValue(args, "limit", 20), 1, 20);
        return ticketResult(mapper.findTickets(user.id(), status, limit));
    }

    private ToolResult ticketResult(List<AssistantToolMapper.TicketToolRow> rows) {
        List<ResourceCard> resources = rows.stream()
                .map(row -> new ResourceCard(
                        "TICKET",
                        row.id(),
                        row.activityTitle(),
                        row.status() + " · " + seatDescription(row),
                        "/tickets/" + row.id()))
                .toList();
        return result(rows, resources);
    }

    private String seatDescription(AssistantToolMapper.TicketToolRow row) {
        return Stream.of(row.areaName(), row.rowLabel(), row.seatNumber())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(row.ticketTypeName());
    }

    private ToolResult result(Object data, List<ResourceCard> resources) {
        try {
            return new ToolResult(objectMapper.writeValueAsString(data), resources);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize assistant tool result", exception);
        }
    }

    private JsonNode readArguments(String arguments) {
        try {
            return objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 工具参数格式错误");
        }
    }

    private LocalDate dateValue(JsonNode args, String field) {
        String value = text(args, field);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "日期格式应为 YYYY-MM-DD");
        }
    }

    private long requiredLong(JsonNode args, String field) {
        Long value = longValue(args, field);
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, field + " 不正确");
        }
        return value;
    }

    private Long longValue(JsonNode args, String field) {
        JsonNode value = args.get(field);
        return value == null || !value.canConvertToLong() ? null : value.longValue();
    }

    private int intValue(JsonNode args, String field, int defaultValue) {
        JsonNode value = args.get(field);
        return value == null || !value.canConvertToInt() ? defaultValue : value.intValue();
    }

    private String text(JsonNode args, String field) {
        JsonNode value = args.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            return null;
        }
        return value.textValue().trim();
    }

    private Map<String, Object> definition(String name, String description, Map<String, Object> parameters) {
        return Map.of(
                "type",
                "function",
                "function",
                Map.of("name", name, "description", description, "parameters", parameters));
    }

    @SafeVarargs
    private final Map<String, Object> properties(Map.Entry<String, Object>... entries) {
        return requiredProperties(List.of(), entries);
    }

    @SafeVarargs
    private final Map<String, Object> requiredProperties(List<String> required, Map.Entry<String, Object>... entries) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            properties.put(entry.getKey(), entry.getValue());
        }
        return Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false);
    }

    private Map.Entry<String, Object> property(String name, String type, String description) {
        return Map.entry(name, Map.of("type", type, "description", description));
    }

    public record ToolResult(String json, List<ResourceCard> resources) {}
}
