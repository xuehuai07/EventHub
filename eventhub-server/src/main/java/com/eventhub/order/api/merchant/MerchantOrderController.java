package com.eventhub.order.api.merchant;

import com.eventhub.common.api.ApiResponse;
import com.eventhub.common.api.PageResponse;
import com.eventhub.order.application.order.OrderQueryService;
import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.dto.OrderView;
import com.eventhub.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/orders")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantOrderController {

    private final OrderQueryService service;

    public MerchantOrderController(OrderQueryService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResponse<OrderView>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.merchantOrders(user, status, keyword, page, pageSize));
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderView> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long orderId) {
        return ApiResponse.success(service.merchantOrder(user, orderId));
    }
}
