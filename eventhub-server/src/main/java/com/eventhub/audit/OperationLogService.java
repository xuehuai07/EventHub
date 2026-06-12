package com.eventhub.audit;

import com.eventhub.common.api.PageResponse;
import com.eventhub.common.request.RequestIdFilter;
import com.eventhub.security.AuthenticatedUser;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service
public class OperationLogService {

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(
            AuthenticatedUser user,
            Long merchantId,
            String action,
            String resourceType,
            long resourceId,
            String summary) {
        String role = user.roles().contains("ADMIN") ? "ADMIN" : "MERCHANT";
        mapper.insert(
                user.id(),
                user.displayName(),
                role,
                merchantId,
                action,
                resourceType,
                resourceId,
                truncate(summary),
                currentRequestId());
    }

    public PageResponse<OperationLogView> list(
            Long operatorUserId,
            String action,
            String resourceType,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        String normalizedAction = normalize(action);
        String normalizedType = normalize(resourceType);
        return PageResponse.of(
                mapper.find(
                        operatorUserId,
                        normalizedAction,
                        normalizedType,
                        startDate,
                        endDate,
                        (safePage - 1) * safeSize,
                        safeSize),
                safePage,
                safeSize,
                mapper.count(operatorUserId, normalizedAction, normalizedType, startDate, endDate));
    }

    private String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    private String truncate(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
