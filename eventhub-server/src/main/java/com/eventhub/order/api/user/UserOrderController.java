package com.eventhub.order.api.user;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.order.application.order.OrderCreationService;
import com.eventhub.order.application.order.OrderQueryService;
import com.eventhub.order.application.payment.OrderActionService;
import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.dto.CreateOrderRequest;
import com.eventhub.order.dto.OrderView;
import com.eventhub.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('USER')")
public class UserOrderController {

    private final OrderCreationService creation;
    private final OrderQueryService queries;
    private final OrderActionService actions;

    public UserOrderController(OrderCreationService creation, OrderQueryService queries, OrderActionService actions) {
        this.creation = creation;
        this.queries = queries;
        this.actions = actions;
    }

    @PostMapping
    ApiResponse<OrderView> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(creation.create(user, request, idempotencyKey));
    }

    @GetMapping
    ApiResponse<PageResponse<OrderView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(queries.userOrders(user, status, page, pageSize));
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderView> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long orderId) {
        return ApiResponse.success(queries.userOrder(user, orderId));
    }

    @PostMapping("/{orderId}/pay")
    ApiResponse<OrderView> pay(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long orderId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.success(actions.pay(user, orderId, idempotencyKey));
    }

    @PostMapping("/{orderId}/cancel")
    ApiResponse<OrderView> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long orderId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.success(actions.cancel(user, orderId, idempotencyKey));
    }
}
