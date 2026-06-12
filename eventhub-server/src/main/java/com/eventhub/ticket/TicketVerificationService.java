package com.eventhub.ticket;

import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.user.MerchantContextService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketVerificationService {

    private final TicketMapper tickets;
    private final TicketCredentialService credentials;
    private final MerchantContextService merchantContext;

    public TicketVerificationService(
            TicketMapper tickets, TicketCredentialService credentials, MerchantContextService merchantContext) {
        this.tickets = tickets;
        this.credentials = credentials;
        this.merchantContext = merchantContext;
    }

    public VerificationResultView preview(AuthenticatedUser user, VerificationRequest request) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        TicketRecord ticket = resolve(request.code());
        requireMerchant(ticket, merchantId);
        return result(ticket, false);
    }

    @Transactional
    public VerificationResultView verify(AuthenticatedUser user, VerificationRequest request, String requestIp) {
        long merchantId = merchantContext.requireActiveMerchant(user).merchantId();
        TicketRecord ticket = resolve(request.code());
        requireMerchant(ticket, merchantId);
        LocalDateTime now = LocalDateTime.now();
        boolean success = tickets.markUsed(ticket.id(), merchantId, user.id(), normalize(request.deviceId()), now) == 1;
        TicketRecord current = tickets.findById(ticket.id());
        if (!success && !"USED".equals(current.status())) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_INVALID);
        }
        tickets.insertVerificationLog(
                ticket.id(),
                merchantId,
                user.id(),
                success ? "SUCCESS" : "ALREADY_USED",
                normalize(request.deviceId()),
                normalize(requestIp),
                now);
        return result(current, success);
    }

    public PageResponse<VerificationLogView> logs(AuthenticatedUser user, boolean admin, int page, int pageSize) {
        Long merchantId =
                admin ? null : merchantContext.requireActiveMerchant(user).merchantId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        return PageResponse.of(
                tickets.findVerificationLogs(merchantId, (safePage - 1) * safeSize, safeSize),
                safePage,
                safeSize,
                tickets.countVerificationLogs(merchantId));
    }

    private TicketRecord resolve(String code) {
        TicketRecord ticket = tickets.findByTicketNo(credentials.resolveTicketNo(code.trim()));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private void requireMerchant(TicketRecord ticket, long merchantId) {
        if (ticket.merchantId() != merchantId) {
            throw new BusinessException(ErrorCode.TICKET_MERCHANT_MISMATCH);
        }
    }

    private VerificationResultView result(TicketRecord ticket, boolean success) {
        return new VerificationResultView(
                TicketView.from(ticket),
                success,
                "USED".equals(ticket.status()) && !success,
                ticket.usedAt(),
                ticket.verifierName());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
