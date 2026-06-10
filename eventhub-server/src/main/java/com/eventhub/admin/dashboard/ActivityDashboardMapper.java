package com.eventhub.admin.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityDashboardMapper {

    @Select("""
            SELECT
                (SELECT COUNT(*) FROM eh_merchant WHERE status = 'ACTIVE') AS merchant_count,
                (SELECT COUNT(*) FROM eh_activity WHERE status = 'DRAFT') AS draft_count,
                (SELECT COUNT(*) FROM eh_activity WHERE status = 'PENDING_REVIEW') AS pending_review_count,
                (SELECT COUNT(*) FROM eh_activity WHERE status = 'PUBLISHED') AS published_count,
                (SELECT COUNT(*) FROM eh_activity_session
                 WHERE status = 'SCHEDULED' AND start_at > CURRENT_TIMESTAMP(3)) AS upcoming_session_count
            """)
    ActivityDashboardSummary summary();
}
