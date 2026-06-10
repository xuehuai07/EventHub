package com.eventhub.admin.dashboard;

public record ActivityDashboardSummary(
        long merchantCount, long draftCount, long pendingReviewCount, long publishedCount, long upcomingSessionCount) {}
