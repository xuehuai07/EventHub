package com.eventhub.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TicketCredentialServiceTests {

    private static final String SECRET = "test-ticket-secret-that-is-long-enough-for-hmac";

    @Test
    void issuesAndValidatesCredential() {
        TicketCredentialService service = new TicketCredentialService(SECRET, Duration.ofMinutes(1));
        TicketCredentialView credential = service.issue(ticket());

        assertThat(service.resolveTicketNo(credential.credential())).isEqualTo("ET1234567890abcdef1234567890abcdef");
    }

    @Test
    void rejectsTamperedAndExpiredCredentials() {
        TicketCredentialService service = new TicketCredentialService(SECRET, Duration.ofMinutes(1));
        String credential = service.issue(ticket()).credential();

        assertThatThrownBy(() -> service.resolveTicketNo(credential + "x"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_CREDENTIAL_INVALID);

        TicketCredentialService expired = new TicketCredentialService(SECRET, Duration.ofSeconds(-1));
        assertThatThrownBy(() -> expired.resolveTicketNo(expired.issue(ticket()).credential()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_CREDENTIAL_EXPIRED);
    }

    private TicketRecord ticket() {
        return new TicketRecord(
                1,
                "ET1234567890abcdef1234567890abcdef",
                2,
                3,
                "UNUSED",
                null,
                null,
                null,
                "EH1",
                4,
                "活动",
                "场次",
                "场馆",
                "标准票",
                null,
                null,
                null,
                LocalDateTime.now(),
                null,
                "用户",
                null);
    }
}
