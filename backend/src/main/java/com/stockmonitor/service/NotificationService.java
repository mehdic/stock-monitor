package com.stockmonitor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.dto.NotificationDTO;
import com.stockmonitor.model.Notification;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.NotificationRepository;
import com.stockmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notification service for managing user notifications per FR-045, FR-048, FR-049.
 *
 * T104: Implements notification creation, delivery, read tracking, and user preferences.
 *
 * Features:
 * - Create and send notifications with categories and priorities
 * - Broadcast notifications via WebSocket
 * - Track read status per FR-048
 * - Respect user preferences for opt-out per category per FR-049
 * - Support multiple notification types per FR-045
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketService;
    private final ObjectMapper objectMapper;

    /**
     * Create and send notification to user.
     *
     * @param userId User ID
     * @param title Notification title
     * @param message Notification message
     * @param category Notification category (T-3_PRECOMPUTE, T-1_STAGED, etc.)
     * @param priority Priority level (HIGH, MEDIUM, LOW)
     * @param actionLabel Action button label
     * @param actionUrl Action URL
     * @return Created notification DTO, or null if user opted out
     */
    @Transactional
    public NotificationDTO createNotification(
            UUID userId,
            String title,
            String message,
            String category,
            String priority,
            String actionLabel,
            String actionUrl
    ) {
        // Check user preferences per FR-049
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!shouldSendNotification(user, category)) {
            log.debug("User {} opted out of {} notifications. Skipping.", userId, category);
            return null;
        }

        // Create notification entity
        Notification notification = Notification.builder()
                .userId(userId)
                .category(category)
                .priority(priority)
                .title(title)
                .subject(title) // Use title as subject for backwards compatibility
                .message(message)
                .actionLabel(actionLabel)
                .actionUrl(actionUrl)
                .notificationType(category) // Map category to notificationType
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .deliveryStatus("SENT")
                .build();

        notification = notificationRepository.save(notification);

        // Convert to DTO
        NotificationDTO dto = toDTO(notification);

        // Broadcast via WebSocket per FR-046
        try {
            webSocketService.broadcastNotification(userId, dto);
            log.debug("Notification {} broadcast to user {} via WebSocket", notification.getId(), userId);
        } catch (Exception e) {
            log.error("Failed to broadcast notification via WebSocket: {}", e.getMessage());
        }

        log.info("Created {} notification for user {}: {}", category, userId, title);
        return dto;
    }

    /**
     * Get all notifications for user, ordered by created date descending.
     *
     * @param userId User ID
     * @return List of notifications
     */
    public List<NotificationDTO> getNotificationsForUser(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for user.
     *
     * @param userId User ID
     * @return List of unread notifications
     */
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mark notification as read per FR-048.
     *
     * @param notificationId Notification ID
     */
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        log.debug("Marked notification {} as read", notificationId);
    }

    /**
     * Mark all notifications as read for user.
     *
     * @param userId User ID
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);

        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(now);
        }

        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
    }

    /**
     * Get unread notification count for user.
     *
     * @param userId User ID
     * @return Unread count
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Send T-3 pre-compute notification.
     *
     * @param userId User ID
     * @param runId Run ID
     */
    public void sendT3PreComputeNotification(UUID userId, UUID runId) {
        createNotification(
                userId,
                "Month-End Recommendations: T-3 Pre-Compute Started",
                "Pre-compute job has started for your month-end recommendations. Data is being prepared for optimization.",
                "T-3_PRECOMPUTE",
                "MEDIUM",
                "View Status",
                "/runs/" + runId
        );
    }

    /**
     * Send T-1 staged notification.
     *
     * @param userId User ID
     * @param runId Run ID
     */
    public void sendT1StagedNotification(UUID userId, UUID runId) {
        createNotification(
                userId,
                "Month-End Recommendations: T-1 Staging Complete",
                "Data freshness checks completed. Your recommendations are staged and ready for finalization.",
                "T-1_STAGED",
                "MEDIUM",
                "View Status",
                "/runs/" + runId
        );
    }

    /**
     * Send T finalized notification (HIGH priority per FR-045).
     *
     * @param userId User ID
     * @param runId Run ID
     */
    public void sendTFinalizedNotification(UUID userId, UUID runId) {
        createNotification(
                userId,
                "Month-End Recommendations Ready!",
                "Your month-end portfolio recommendations are now available. Review your personalized picks and download your report.",
                "T_FINALIZED",
                "HIGH",
                "View Recommendations",
                "/recommendations?runId=" + runId
        );
    }

    /**
     * Send data stale notification (HIGH priority per FR-026).
     *
     * @param userId User ID
     */
    public void sendDataStaleNotification(UUID userId) {
        createNotification(
                userId,
                "Data Freshness Warning",
                "Some data used for your recommendations is older than 48 hours. Recommendations may be less accurate. Please review data sources.",
                "DATA_STALE",
                "HIGH",
                "Review Data",
                "/settings/data-sources"
        );
    }

    /**
     * Send run failed notification.
     *
     * @param userId User ID
     * @param runId Run ID
     * @param errorMessage Error message
     */
    public void sendRunFailedNotification(UUID userId, UUID runId, String errorMessage) {
        createNotification(
                userId,
                "Recommendation Run Failed",
                "Your recommendation run failed: " + errorMessage + ". Please contact support if this issue persists.",
                "RUN_FAILED",
                "HIGH",
                "View Details",
                "/runs/" + runId
        );
    }

    /**
     * Check if notification should be sent based on user preferences per FR-049.
     *
     * @param user User entity
     * @param category Notification category
     * @return True if notification should be sent
     */
    private boolean shouldSendNotification(User user, String category) {
        String preferencesJson = user.getNotificationPreferences();

        if (preferencesJson == null || preferencesJson.isEmpty()) {
            // Default: all categories enabled
            return true;
        }

        try {
            Map<String, Boolean> preferences = objectMapper.readValue(
                    preferencesJson,
                    new TypeReference<Map<String, Boolean>>() {}
            );

            // Check if user opted out of this category
            return preferences.getOrDefault(category, true);
        } catch (Exception e) {
            log.error("Failed to parse notification preferences for user {}: {}", user.getId(), e.getMessage());
            // Default to sending notification if preferences can't be parsed
            return true;
        }
    }

    /**
     * Convert Notification entity to DTO.
     *
     * @param notification Notification entity
     * @return Notification DTO
     */
    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .category(notification.getCategory())
                .priority(notification.getPriority())
                .title(notification.getTitle() != null ? notification.getTitle() : notification.getSubject())
                .message(notification.getMessage())
                .actionLabel(notification.getActionLabel())
                .actionUrl(notification.getActionUrl())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
