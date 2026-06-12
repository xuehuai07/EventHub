package com.eventhub.ticket;

import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserTicketService {

    private final TicketMapper tickets;
    private final TicketCredentialService credentials;

    public UserTicketService(TicketMapper tickets, TicketCredentialService credentials) {
        this.tickets = tickets;
        this.credentials = credentials;
    }

    public PageResponse<TicketView> list(AuthenticatedUser user, String status, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        List<TicketView> items =
                tickets
                        .findUserTickets(user.id(), normalizeStatus(status), (safePage - 1) * safeSize, safeSize)
                        .stream()
                        .map(TicketView::from)
                        .toList();
        return PageResponse.of(items, safePage, safeSize, tickets.countUserTickets(user.id(), normalizeStatus(status)));
    }

    public TicketView detail(AuthenticatedUser user, long ticketId) {
        return TicketView.from(requireOwned(user, ticketId));
    }

    public List<TicketView> orderTickets(AuthenticatedUser user, long orderId) {
        return tickets.findOrderTickets(orderId, user.id()).stream()
                .map(TicketView::from)
                .toList();
    }

    public TicketCredentialView credential(AuthenticatedUser user, long ticketId) {
        TicketRecord ticket = requireOwned(user, ticketId);
        if (!"UNUSED".equals(ticket.status())) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_INVALID);
        }
        return credentials.issue(ticket);
    }

    private TicketRecord requireOwned(AuthenticatedUser user, long ticketId) {
        TicketRecord ticket = tickets.findById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        if (ticket.userId() != user.id()) {
            throw new BusinessException(ErrorCode.TICKET_ACCESS_DENIED);
        }
        return ticket;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!List.of("UNUSED", "USED", "CANCELLED").contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "票券状态不正确");
        }
        return status;
    }
}
