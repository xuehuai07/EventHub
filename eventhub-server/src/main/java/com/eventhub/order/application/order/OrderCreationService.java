package com.eventhub.order.application.order;

import com.eventhub.activity.domain.SeatMode;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.order.dto.CreateOrderRequest;
import com.eventhub.order.dto.OrderView;
import com.eventhub.order.infrastructure.idempotency.IdempotencyGuard;
import com.eventhub.order.infrastructure.persistence.AvailabilityMapper;
import com.eventhub.order.infrastructure.persistence.OrderCommandMapper;
import com.eventhub.order.infrastructure.persistence.OrderItemRecord;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import com.eventhub.order.infrastructure.persistence.SaleContextRecord;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockRecord;
import com.eventhub.order.infrastructure.redis.FixedSeatLockStore;
import com.eventhub.order.infrastructure.redis.GeneralStockLockStore;
import com.eventhub.security.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderCreationService {

    private static final String SCOPE = "CREATE_ORDER";

    private final SeatLockMapper locks;
    private final AvailabilityMapper availability;
    private final OrderCommandMapper commands;
    private final OrderQueryMapper queries;
    private final OrderViewAssembler assembler;
    private final IdempotencyGuard guard;
    private final FixedSeatLockStore fixedLocks;
    private final GeneralStockLockStore stockLocks;

    public OrderCreationService(
            SeatLockMapper locks,
            AvailabilityMapper availability,
            OrderCommandMapper commands,
            OrderQueryMapper queries,
            OrderViewAssembler assembler,
            IdempotencyGuard guard,
            FixedSeatLockStore fixedLocks,
            GeneralStockLockStore stockLocks) {
        this.locks = locks;
        this.availability = availability;
        this.commands = commands;
        this.queries = queries;
        this.assembler = assembler;
        this.guard = guard;
        this.fixedLocks = fixedLocks;
        this.stockLocks = stockLocks;
    }

    @Transactional
    public OrderView create(AuthenticatedUser user, CreateOrderRequest request, String idempotencyKey) {
        requireKey(idempotencyKey);
        String requestHash = hash(request.lockNo());
        Long existingId = commands.findIdempotencyResource(user.id(), SCOPE, idempotencyKey);
        if (existingId != null) {
            requireSameHash(user.id(), idempotencyKey, requestHash);
            return assembler.view(queries.findById(existingId));
        }
        if (!guard.acquire(user.id(), SCOPE, idempotencyKey)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "请求正在处理中，请勿重复提交");
        }
        releaseGuardAfterCompletion(user.id(), idempotencyKey);
        return createNew(user, request.lockNo(), idempotencyKey, requestHash);
    }

    private OrderView createNew(AuthenticatedUser user, String lockNo, String key, String requestHash) {
        SeatLockRecord lock = locks.findByLockNo(lockNo);
        if (lock == null) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_NOT_FOUND);
        }
        if (lock.userId() != user.id()) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_ACCESS_DENIED);
        }
        if (!"ACTIVE".equals(lock.status()) || !LocalDateTime.now().isBefore(lock.expiresAt())) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_EXPIRED);
        }
        SaleContextRecord context = availability.findSaleContexts(lock.sessionId()).stream()
                .filter(item -> item.ticketTypeId() == lock.ticketTypeId())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_AVAILABLE));
        List<Long> seatIds = locks.findSeatIds(lock.id());
        if (lock.seatMode() == SeatMode.FIXED) {
            if (commands.sellSeats(seatIds) != lock.quantity()) {
                throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
            }
        } else if (commands.deductStock(lock.ticketTypeId(), lock.quantity()) != 1) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        String orderNo = "EH" + UUID.randomUUID().toString().replace("-", "");
        OrderRecord draft = new OrderRecord(
                null,
                orderNo,
                key,
                user.id(),
                context.merchantId(),
                context.activityId(),
                context.sessionId(),
                lock.id(),
                null,
                context.priceCents() * lock.quantity(),
                lock.quantity(),
                LocalDateTime.now().plusMinutes(15),
                null,
                null,
                context.activityTitle(),
                context.sessionName(),
                context.venueName());
        commands.insertOrder(draft);
        OrderRecord order = queries.findByOrderNo(orderNo);
        commands.insertItems(items(order.id(), context, lock, seatIds));
        if (locks.updateStatus(lock.id(), "CONSUMED", order.id()) != 1) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_EXPIRED);
        }
        commands.insertIdempotency(
                user.id(),
                SCOPE,
                key,
                requestHash,
                "ORDER",
                order.id(),
                LocalDateTime.now().plusDays(1));
        releaseLockAfterCommit(lock, seatIds);
        return assembler.view(queries.findById(order.id()));
    }

    private List<OrderItemRecord> items(
            long orderId, SaleContextRecord context, SeatLockRecord lock, List<Long> seatIds) {
        if (lock.seatMode() == SeatMode.GENERAL) {
            return List.of(new OrderItemRecord(
                    null,
                    orderId,
                    lock.ticketTypeId(),
                    null,
                    lock.quantity(),
                    context.priceCents(),
                    context.priceCents() * lock.quantity(),
                    context.activityTitle(),
                    context.sessionName(),
                    context.venueName(),
                    context.ticketTypeName(),
                    null,
                    null,
                    null));
        }
        return availability.findSeatsByIds(lock.sessionId(), seatIds).stream()
                .map(seat -> new OrderItemRecord(
                        null,
                        orderId,
                        lock.ticketTypeId(),
                        seat.id(),
                        1,
                        context.priceCents(),
                        context.priceCents(),
                        context.activityTitle(),
                        context.sessionName(),
                        context.venueName(),
                        context.ticketTypeName(),
                        seat.areaName(),
                        seat.rowLabel(),
                        seat.seatNumber()))
                .toList();
    }

    private void releaseLockAfterCommit(SeatLockRecord lock, List<Long> seatIds) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (lock.seatMode() == SeatMode.FIXED) {
                    fixedLocks.release(lock.sessionId(), seatIds, lock.lockNo());
                } else {
                    stockLocks.release(lock.ticketTypeId(), lock.lockNo());
                }
            }
        });
    }

    private void releaseGuardAfterCompletion(long userId, String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                guard.release(userId, SCOPE, key);
            }
        });
    }

    private void requireSameHash(long userId, String key, String expectedHash) {
        String actualHash = commands.findIdempotencyHash(userId, SCOPE, key);
        if (!expectedHash.equals(actualHash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }

    private void requireKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9-]{8,64}")) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
