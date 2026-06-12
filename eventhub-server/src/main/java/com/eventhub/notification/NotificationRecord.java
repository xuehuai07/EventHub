package com.eventhub.notification;

import java.time.LocalDateTime;

public class NotificationRecord {

    private Long id;
    private long userId;
    private String type;
    private String title;
    private String content;
    private String resourceType;
    private Long resourceId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public NotificationRecord(
            long userId, String type, String title, String content, String resourceType, Long resourceId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
