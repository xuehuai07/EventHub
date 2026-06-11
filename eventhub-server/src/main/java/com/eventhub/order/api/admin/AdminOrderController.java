package com.eventhub.order.api.admin;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.order.application.order.OrderQueryService;
import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.dto.OrderView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderQueryService service;

    public AdminOrderController(OrderQueryService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResponse<OrderView>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.adminOrders(status, keyword, page, pageSize));
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderView> detail(@PathVariable long orderId) {
        return ApiResponse.success(service.adminOrder(orderId));
    }
}
