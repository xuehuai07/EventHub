package com.eventhub.order.application.payment;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.order.application.order.InventoryRestorer;
import com.eventhub.order.application.order.OrderViewAssembler;
import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.dto.OrderView;
import com.eventhub.order.infrastructure.outbox.OrderOutboxService;
import com.eventhub.order.infrastructure.persistence.OrderCommandMapper;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
import com.eventhub.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderActionService {

    private final OrderQueryMapper queries;
    private final OrderCommandMapper commands;
    private final SeatLockMapper locks;
    private final InventoryRestorer inventory;
    private final OrderViewAssembler assembler;
    private final OrderOutboxService outbox;

    public OrderActionService(
            OrderQueryMapper queries,
            OrderCommandMapper commands,
            SeatLockMapper locks,
            InventoryRestorer inventory,
            OrderViewAssembler assembler,
            OrderOutboxService outbox) {
        this.queries = queries;
        this.commands = commands;
        this.locks = locks;
        this.inventory = inventory;
        this.assembler = assembler;
        this.outbox = outbox;
    }

    @Transactional
    public OrderView pay(AuthenticatedUser user, long orderId, String idempotencyKey) {
        requireKey(idempotencyKey);
        OrderView repeated = repeated(user, "PAY_ORDER", orderId, idempotencyKey);
        if (repeated != null) {
            return repeated;
        }
        OrderRecord order = requireOwned(user, orderId);
        if (order.status() == OrderStatus.PAID) {
            return assembler.view(order);
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (!LocalDateTime.now().isBefore(order.paymentDeadlineAt())) {
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_EXPIRED);
        }
        if (commands.transition(order.id(), "PAID") != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        commands.insertPayment(
                "PAY" + UUID.randomUUID().toString().replace("-", ""), order.id(), order.totalAmountCents());
        recordAction(user, "PAY_ORDER", orderId, idempotencyKey);
        OrderRecord paidOrder = queries.findById(orderId);
        outbox.appendPaid(paidOrder);
        return assembler.view(paidOrder);
    }

    @Transactional
    public OrderView cancel(AuthenticatedUser user, long orderId, String idempotencyKey) {
        requireKey(idempotencyKey);
        OrderView repeated = repeated(user, "CANCEL_ORDER", orderId, idempotencyKey);
        if (repeated != null) {
            return repeated;
        }
        OrderRecord order = requireOwned(user, orderId);
        if (order.status() == OrderStatus.CANCELLED) {
            return assembler.view(order);
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (commands.transition(order.id(), "CANCELLED") != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        inventory.restore(locks.findById(order.lockId()));
        recordAction(user, "CANCEL_ORDER", orderId, idempotencyKey);
        return assembler.view(queries.findById(orderId));
    }

    @Transactional
    public void closeExpired(long orderId) {
        OrderRecord order = queries.findById(orderId);
        if (order != null && commands.expire(order.id(), LocalDateTime.now()) == 1) {
            inventory.restore(locks.findById(order.lockId()));
        }
    }

    private OrderRecord requireOwned(AuthenticatedUser user, long orderId) {
        OrderRecord order = queries.findById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.userId() != user.id()) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private void requireKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9-]{8,64}")) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private OrderView repeated(AuthenticatedUser user, String scope, long orderId, String key) {
        Long resourceId = commands.findIdempotencyResource(user.id(), scope, key);
        if (resourceId == null) {
            return null;
        }
        if (resourceId != orderId) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return assembler.view(requireOwned(user, orderId));
    }

    private void recordAction(AuthenticatedUser user, String scope, long orderId, String key) {
        commands.insertIdempotency(
                user.id(),
                scope,
                key,
                Long.toString(orderId),
                "ORDER",
                orderId,
                LocalDateTime.now().plusDays(1));
    }
}
