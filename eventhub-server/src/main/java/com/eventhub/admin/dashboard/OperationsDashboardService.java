package com.eventhub.admin.dashboard;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationsDashboardService {

    private final OperationsDashboardMapper mapper;

    public OperationsDashboardService(OperationsDashboardMapper mapper) {
        this.mapper = mapper;
    }

    public OperationsDashboardView operations(Long merchantId) {
        return mapper.operations(merchantId);
    }

    public List<SalesTrendView> salesTrend(Long merchantId, LocalDate startDate, LocalDate endDate) {
        LocalDate safeEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate safeStart = startDate == null ? safeEnd.minusDays(29) : startDate;
        if (safeStart.isAfter(safeEnd) || ChronoUnit.DAYS.between(safeStart, safeEnd) > 365) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "统计日期范围不正确或超过 366 天");
        }
        return mapper.salesTrend(merchantId, safeStart, safeEnd);
    }

    public List<TopActivityView> topActivities(Long merchantId, int limit) {
        return mapper.topActivities(merchantId, Math.clamp(limit, 1, 20));
    }
}
