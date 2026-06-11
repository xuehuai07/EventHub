package com.eventhub.order.application.order;

import com.eventhub.activity.domain.SeatMode;
import com.eventhub.order.infrastructure.persistence.OrderCommandMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockMapper;
import com.eventhub.order.infrastructure.persistence.SeatLockRecord;
import org.springframework.stereotype.Component;

@Component
public class InventoryRestorer {

    private final SeatLockMapper locks;
    private final OrderCommandMapper commands;

    public InventoryRestorer(SeatLockMapper locks, OrderCommandMapper commands) {
        this.locks = locks;
        this.commands = commands;
    }

    public void restore(SeatLockRecord lock) {
        if (lock.seatMode() == SeatMode.FIXED) {
            var seatIds = locks.findSeatIds(lock.id());
            if (!seatIds.isEmpty()) {
                commands.restoreSeats(seatIds);
            }
        } else {
            commands.restoreStock(lock.ticketTypeId(), lock.quantity());
        }
    }
}
