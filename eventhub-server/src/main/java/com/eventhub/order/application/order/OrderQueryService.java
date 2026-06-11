package com.eventhub.order.application.order;

import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.dto.OrderView;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.user.MerchantContextService;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryService {

    private final OrderQueryMapper queries;
    private final OrderViewAssembler assembler;
    private final MerchantContextService merchantContext;

    public OrderQueryService(
            OrderQueryMapper queries, OrderViewAssembler assembler, MerchantContextService merchantContext) {
        this.queries = queries;
        this.assembler = assembler;
        this.merchantContext = merchantContext;
    }

    public PageResponse<OrderView> userOrders(AuthenticatedUser user, OrderStatus status, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        return PageResponse.of(
                assembler.summaries(queries.findUserOrders(user.id(), status, (safePage - 1) * safeSize, safeSize)),
                safePage,
                safeSize,
                queries.countUserOrders(user.id(), status));
    }

    public OrderView userOrder(AuthenticatedUser user, long orderId) {
        OrderRecord order = require(orderId);
        if (order.userId() != user.id()) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return assembler.view(order);
    }

    public PageResponse<OrderView> merchantOrders(
            AuthenticatedUser user, OrderStatus status, String keyword, int page, int pageSize) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        return managedOrders(merchantId, status, keyword, page, pageSize);
    }

    public OrderView merchantOrder(AuthenticatedUser user, long orderId) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        OrderRecord order = require(orderId);
        if (order.merchantId() != merchantId) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return assembler.view(order);
    }

    public PageResponse<OrderView> adminOrders(OrderStatus status, String keyword, int page, int pageSize) {
        return managedOrders(null, status, keyword, page, pageSize);
    }

    public OrderView adminOrder(long orderId) {
        return assembler.view(require(orderId));
    }

    private PageResponse<OrderView> managedOrders(
            Long merchantId, OrderStatus status, String keyword, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.of(
                assembler.summaries(
                        queries.findManagedOrders(merchantId, status, normalized, (safePage - 1) * safeSize, safeSize)),
                safePage,
                safeSize,
                queries.countManagedOrders(merchantId, status, normalized));
    }

    private OrderRecord require(long orderId) {
        OrderRecord order = queries.findById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }
}
