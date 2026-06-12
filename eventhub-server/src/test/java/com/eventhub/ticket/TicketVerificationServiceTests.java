package com.eventhub.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import com.eventhub.security.ClientType;
import com.eventhub.user.MerchantBinding;
import com.eventhub.user.MerchantContextService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketVerificationServiceTests {

    @Mock
    private TicketMapper tickets;

    @Mock
    private TicketCredentialService credentials;

    @Mock
    private MerchantContextService merchantContext;

    private TicketVerificationService service;
    private final AuthenticatedUser user = new AuthenticatedUser(
            7, "merchant", null, "核销员", List.of("MERCHANT"), List.of(), ClientType.ADMIN_WEB, "s");

    @BeforeEach
    void setUp() {
        service = new TicketVerificationService(tickets, credentials, merchantContext);
        when(merchantContext.requireActiveMerchant(user)).thenReturn(new MerchantBinding(9, "商家", "ACTIVE", "ACTIVE"));
        when(credentials.resolveTicketNo("code")).thenReturn("ET1234567890abcdef1234567890abcdef");
    }

    @Test
    void verifiesTicketAndWritesAuditLog() {
        TicketRecord unused = ticket(9, "UNUSED", null, null);
        TicketRecord used = ticket(9, "USED", LocalDateTime.now(), "核销员");
        when(tickets.findByTicketNo(anyString())).thenReturn(unused);
        when(tickets.markUsed(anyLong(), anyLong(), anyLong(), any(), any())).thenReturn(1);
        when(tickets.findById(1)).thenReturn(used);

        VerificationResultView result = service.verify(user, new VerificationRequest("code", "device"), "127.0.0.1");

        assertThat(result.success()).isTrue();
        assertThat(result.alreadyUsed()).isFalse();
        verify(tickets)
                .insertVerificationLog(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(9L),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("SUCCESS"),
                        org.mockito.ArgumentMatchers.eq("device"),
                        org.mockito.ArgumentMatchers.eq("127.0.0.1"),
                        any(LocalDateTime.class));
    }

    @Test
    void repeatedVerificationReturnsExistingUsageAndAuditsAttempt() {
        TicketRecord used = ticket(9, "USED", LocalDateTime.now(), "其他核销员");
        when(tickets.findByTicketNo(anyString())).thenReturn(used);
        when(tickets.markUsed(anyLong(), anyLong(), anyLong(), any(), any())).thenReturn(0);
        when(tickets.findById(1)).thenReturn(used);

        VerificationResultView result = service.verify(user, new VerificationRequest("code", null), "127.0.0.1");

        assertThat(result.success()).isFalse();
        assertThat(result.alreadyUsed()).isTrue();
        verify(tickets)
                .insertVerificationLog(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(9L),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("ALREADY_USED"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.eq("127.0.0.1"),
                        any(LocalDateTime.class));
    }

    @Test
    void rejectsTicketFromAnotherMerchant() {
        when(tickets.findByTicketNo(anyString())).thenReturn(ticket(10, "UNUSED", null, null));

        assertThatThrownBy(() -> service.preview(user, new VerificationRequest("code", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_MERCHANT_MISMATCH);
    }

    private TicketRecord ticket(long merchantId, String status, LocalDateTime usedAt, String verifier) {
        return new TicketRecord(
                1,
                "ET1234567890abcdef1234567890abcdef",
                2,
                3,
                status,
                usedAt,
                usedAt == null ? null : 8L,
                null,
                "EH1",
                merchantId,
                "活动",
                "场次",
                "场馆",
                "标准票",
                "A区",
                "1",
                "2",
                LocalDateTime.now(),
                null,
                "用户",
                verifier);
    }
}
