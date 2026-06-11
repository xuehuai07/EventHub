package com.eventhub.order.application.lock;

import com.eventhub.activity.domain.SeatMode;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.order.dto.SeatLockRequest;
import com.eventhub.order.dto.SeatLockView;
import com.eventhub.order.infrastructure.persistence.AvailabilityMapper;
import com.eventhub.order.infrastructure.persistence.SaleContextRecord;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockRecord;
import com.eventhub.order.infrastructure.persistence.SeatRecord;
import com.eventhub.order.infrastructure.redis.FixedSeatLockStore;
import com.eventhub.order.infrastructure.redis.GeneralStockLockStore;
import com.eventhub.security.AuthenticatedUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatLockService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final AvailabilityMapper availabilityMapper;
    private final SeatLockMapper lockMapper;
    private final FixedSeatLockStore fixedLocks;
    private final GeneralStockLockStore stockLocks;

    public SeatLockService(
            AvailabilityMapper availabilityMapper,
            SeatLockMapper lockMapper,
            FixedSeatLockStore fixedLocks,
            GeneralStockLockStore stockLocks) {
        this.availabilityMapper = availabilityMapper;
        this.lockMapper = lockMapper;
        this.fixedLocks = fixedLocks;
        this.stockLocks = stockLocks;
    }

    @Transactional
    public SeatLockView create(AuthenticatedUser user, SeatLockRequest request) {
        SaleContextRecord context = requireContext(request.sessionId(), request.ticketTypeId());
        requireSaleOpen(context);
        String lockNo = "LK" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plus(LOCK_TTL);
        List<Long> seatIds = request.sessionSeatIds() == null
                ? List.of()
                : request.sessionSeatIds().stream().distinct().toList();
        int quantity = context.seatMode() == SeatMode.FIXED ? seatIds.size() : request.quantity();
        requireQuantity(context, quantity);
        boolean locked = context.seatMode() == SeatMode.FIXED
                ? lockFixed(context, seatIds, lockNo)
                : stockLocks.lock(context.ticketTypeId(), context.availableStock(), quantity, lockNo, LOCK_TTL);
        if (!locked) {
            throw new BusinessException(
                    context.seatMode() == SeatMode.FIXED ? ErrorCode.SEAT_ALREADY_LOCKED : ErrorCode.STOCK_NOT_ENOUGH);
        }
        try {
            SeatLockRecord record = new SeatLockRecord(
                    null,
                    lockNo,
                    user.id(),
                    context.sessionId(),
                    context.ticketTypeId(),
                    context.seatMode(),
                    quantity,
                    "ACTIVE",
                    null,
                    expiresAt);
            lockMapper.insert(record);
            SeatLockRecord saved = lockMapper.findByLockNo(lockNo);
            if (!seatIds.isEmpty()) {
                lockMapper.insertItems(saved.id(), seatIds);
            }
            return view(saved, seatIds, context.priceCents());
        } catch (RuntimeException exception) {
            releaseRedis(context.seatMode(), context.sessionId(), context.ticketTypeId(), seatIds, lockNo);
            throw exception;
        }
    }

    public SeatLockView get(AuthenticatedUser user, String lockNo) {
        SeatLockRecord lock = requireOwned(user, lockNo);
        SaleContextRecord context = requireContext(lock.sessionId(), lock.ticketTypeId());
        return view(lock, lockMapper.findSeatIds(lock.id()), context.priceCents());
    }

    @Transactional
    public SeatLockView release(AuthenticatedUser user, String lockNo) {
        SeatLockRecord lock = requireOwned(user, lockNo);
        List<Long> seatIds = lockMapper.findSeatIds(lock.id());
        if ("ACTIVE".equals(lock.status())) {
            lockMapper.updateStatus(lock.id(), "RELEASED", null);
            releaseRedis(lock.seatMode(), lock.sessionId(), lock.ticketTypeId(), seatIds, lock.lockNo());
            lock = lockMapper.findByLockNo(lockNo);
        }
        SaleContextRecord context = requireContext(lock.sessionId(), lock.ticketTypeId());
        return view(lock, seatIds, context.priceCents());
    }

    private boolean lockFixed(SaleContextRecord context, List<Long> seatIds, String lockNo) {
        if (seatIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }
        List<SeatRecord> seats = availabilityMapper.findSeatsByIds(context.sessionId(), seatIds);
        boolean valid = seats.size() == seatIds.size()
                && seats.stream()
                        .allMatch(seat -> "AVAILABLE".equals(seat.status())
                                && seat.ticketTypeId() != null
                                && seat.ticketTypeId() == context.ticketTypeId());
        if (!valid) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
        }
        return fixedLocks.lock(context.sessionId(), seatIds, lockNo, LOCK_TTL);
    }

    private SaleContextRecord requireContext(long sessionId, long ticketTypeId) {
        return availabilityMapper.findSaleContexts(sessionId).stream()
                .filter(context -> context.ticketTypeId() == ticketTypeId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_AVAILABLE));
    }

    private void requireSaleOpen(SaleContextRecord context) {
        LocalDateTime now = LocalDateTime.now();
        if (!"PUBLISHED".equals(context.activityStatus())
                || !"SCHEDULED".equals(context.sessionStatus())
                || now.isBefore(context.saleStartAt())
                || !now.isBefore(context.saleEndAt())) {
            throw new BusinessException(ErrorCode.SESSION_NOT_AVAILABLE);
        }
    }

    private void requireQuantity(SaleContextRecord context, int quantity) {
        if (quantity < 1 || quantity > context.saleLimitPerUser()) {
            throw new BusinessException(ErrorCode.PURCHASE_LIMIT_EXCEEDED);
        }
    }

    private SeatLockRecord requireOwned(AuthenticatedUser user, String lockNo) {
        SeatLockRecord lock = lockMapper.findByLockNo(lockNo);
        if (lock == null) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_NOT_FOUND);
        }
        if (lock.userId() != user.id()) {
            throw new BusinessException(ErrorCode.SEAT_LOCK_ACCESS_DENIED);
        }
        return lock;
    }

    private SeatLockView view(SeatLockRecord lock, List<Long> seatIds, long unitPrice) {
        String status = "ACTIVE".equals(lock.status()) && !LocalDateTime.now().isBefore(lock.expiresAt())
                ? "EXPIRED"
                : lock.status();
        return new SeatLockView(
                lock.lockNo(),
                lock.sessionId(),
                lock.ticketTypeId(),
                lock.seatMode(),
                lock.quantity(),
                unitPrice * lock.quantity(),
                status,
                lock.expiresAt(),
                seatIds);
    }

    private void releaseRedis(SeatMode mode, long sessionId, long ticketTypeId, List<Long> seatIds, String lockNo) {
        if (mode == SeatMode.FIXED) {
            fixedLocks.release(sessionId, seatIds, lockNo);
        } else {
            stockLocks.release(ticketTypeId, lockNo);
        }
    }
}
