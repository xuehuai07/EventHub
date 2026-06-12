package com.eventhub.ticket;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TicketCredentialService {

    private final byte[] secret;
    private final Duration ttl;

    public TicketCredentialService(
            @Value("${eventhub.ticket.qr-secret}") String secret,
            @Value("${eventhub.ticket.qr-ttl:60s}") Duration ttl) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
    }

    public TicketCredentialView issue(TicketRecord ticket) {
        Instant expiresAt = Instant.now().plus(ttl);
        String payload = String.join(
                "|",
                ticket.ticketNo(),
                Long.toString(ticket.userId()),
                Long.toString(expiresAt.getEpochSecond()),
                UUID.randomUUID().toString());
        String encoded =
                Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new TicketCredentialView(encoded + "." + sign(encoded), expiresAt);
    }

    public String resolveTicketNo(String code) {
        if (code != null && code.matches("ET[0-9a-fA-F]{32}")) {
            return code;
        }
        try {
            String[] parts = code == null ? new String[0] : code.split("\\.", 2);
            if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
                throw new BusinessException(ErrorCode.TICKET_CREDENTIAL_INVALID);
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split("\\|", 4);
            if (values.length != 4) {
                throw new BusinessException(ErrorCode.TICKET_CREDENTIAL_INVALID);
            }
            if (Instant.now().getEpochSecond() > Long.parseLong(values[2])) {
                throw new BusinessException(ErrorCode.TICKET_CREDENTIAL_EXPIRED);
            }
            return values[0];
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.TICKET_CREDENTIAL_INVALID);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("票券凭证签名失败", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
