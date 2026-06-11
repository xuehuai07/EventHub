package com.eventhub.order.application.lock;

import com.eventhub.activity.domain.SeatMode;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.order.dto.AvailabilityView;
import com.eventhub.order.dto.AvailabilityView.SeatAvailability;
import com.eventhub.order.dto.AvailabilityView.TicketAvailability;
import com.eventhub.order.infrastructure.persistence.AvailabilityMapper;
import com.eventhub.order.infrastructure.persistence.SaleContextRecord;
import com.eventhub.order.infrastructure.redis.FixedSeatLockStore;
import com.eventhub.order.infrastructure.redis.GeneralStockLockStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    private final AvailabilityMapper mapper;
    private final FixedSeatLockStore fixedLocks;
    private final GeneralStockLockStore stockLocks;

    public AvailabilityService(
            AvailabilityMapper mapper, FixedSeatLockStore fixedLocks, GeneralStockLockStore stockLocks) {
        this.mapper = mapper;
        this.fixedLocks = fixedLocks;
        this.stockLocks = stockLocks;
    }

    public AvailabilityView get(long sessionId) {
        List<SaleContextRecord> contexts = mapper.findSaleContexts(sessionId);
        if (contexts.isEmpty() || !"PUBLISHED".equals(contexts.getFirst().activityStatus())) {
            throw new BusinessException(ErrorCode.SESSION_NOT_AVAILABLE);
        }
        SaleContextRecord session = contexts.getFirst();
        List<TicketAvailability> tickets = contexts.stream().map(this::ticket).toList();
        List<SeatAvailability> seats = session.seatMode() == SeatMode.FIXED
                ? mapper.findSeats(sessionId).stream()
                        .map(seat -> new SeatAvailability(
                                seat.id(),
                                seat.ticketTypeId() == null ? 0 : seat.ticketTypeId(),
                                seat.areaName(),
                                seat.rowLabel(),
                                seat.seatNumber(),
                                seat.seatGrade(),
                                seat.priceCents(),
                                seatStatus(seat.sessionId(), seat.id(), seat.status())))
                        .toList()
                : List.of();
        return new AvailabilityView(
                session.sessionId(),
                session.activityTitle(),
                session.sessionName(),
                session.venueName(),
                session.seatMode(),
                session.saleStartAt(),
                session.saleEndAt(),
                tickets,
                seats);
    }

    private TicketAvailability ticket(SaleContextRecord context) {
        int locked = context.seatMode() == SeatMode.GENERAL ? stockLocks.activeLocks(context.ticketTypeId()) : 0;
        return new TicketAvailability(
                context.ticketTypeId(),
                context.ticketTypeName(),
                context.seatGrade(),
                context.priceCents(),
                context.totalStock(),
                context.availableStock(),
                locked,
                Math.max(context.availableStock() - locked, 0),
                context.saleLimitPerUser());
    }

    private String seatStatus(long sessionId, long seatId, String databaseStatus) {
        if (!"AVAILABLE".equals(databaseStatus)) {
            return databaseStatus;
        }
        return fixedLocks.isLocked(sessionId, seatId) ? "LOCKED" : "AVAILABLE";
    }
}
