package com.eventhub.notification;

import com.eventhub.common.api.PageResponse;
import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import com.eventhub.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notifications;
    private final SimpMessagingTemplate messaging;

    public NotificationService(NotificationMapper notifications, SimpMessagingTemplate messaging) {
        this.notifications = notifications;
        this.messaging = messaging;
    }

    public void create(long userId, String type, String title, String content, String resourceType, Long resourceId) {
        NotificationRecord notification =
                new NotificationRecord(userId, type, title, content, resourceType, resourceId);
        notifications.insert(notification);
        afterCommit(() -> push(userId, type, notification.getId()));
    }

    public PageResponse<NotificationView> list(AuthenticatedUser user, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.clamp(pageSize, 1, 100);
        return PageResponse.of(
                notifications.findUserNotifications(user.id(), (safePage - 1) * safeSize, safeSize),
                safePage,
                safeSize,
                notifications.countUserNotifications(user.id()));
    }

    public UnreadCountView unreadCount(AuthenticatedUser user) {
        return new UnreadCountView(notifications.countUnread(user.id()));
    }

    public void markRead(AuthenticatedUser user, long notificationId) {
        if (notifications.markRead(notificationId, user.id()) == 0) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    public void markAllRead(AuthenticatedUser user) {
        notifications.markAllRead(user.id());
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void push(long userId, String type, Long notificationId) {
        try {
            messaging.convertAndSendToUser(Long.toString(userId), "/queue/notifications", notificationId);
            messaging.convertAndSendToUser(Long.toString(userId), "/queue/status", type);
        } catch (RuntimeException exception) {
            log.warn("Failed to push notification {} to user {}", notificationId, userId, exception);
        }
    }
}
