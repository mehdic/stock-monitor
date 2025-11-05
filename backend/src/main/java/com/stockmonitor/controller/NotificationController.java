package com.stockmonitor.controller;

import com.stockmonitor.dto.NotificationDTO;
import com.stockmonitor.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for notification management per FR-045, FR-048.
 *
 * T105: Implements REST endpoints for:
 * - GET /api/notifications - Get all notifications for current user
 * - GET /api/notifications/unread - Get unread notifications
 * - GET /api/notifications/unread/count - Get unread count
 * - POST /api/notifications/{id}/read - Mark notification as read
 * - POST /api/notifications/read-all - Mark all as read
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for current user.
     *
     * @param authentication Current user authentication
     * @return List of notifications
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        List<NotificationDTO> notifications = notificationService.getNotificationsForUser(userId);

        log.debug("Retrieved {} notifications for user {}", notifications.size(), userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for current user.
     *
     * @param authentication Current user authentication
     * @return List of unread notifications
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);

        log.debug("Retrieved {} unread notifications for user {}", notifications.size(), userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count for current user.
     *
     * @param authentication Current user authentication
     * @return Unread count
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        long count = notificationService.getUnreadCount(userId);

        log.debug("Unread notification count for user {}: {}", userId, count);
        return ResponseEntity.ok(count);
    }

    /**
     * Mark notification as read.
     *
     * @param id Notification ID
     * @param authentication Current user authentication
     * @return Success response
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = getUserIdFromAuth(authentication);

        // TODO: Verify notification belongs to current user before marking as read
        notificationService.markAsRead(id);

        log.debug("Marked notification {} as read by user {}", id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for current user.
     *
     * @param authentication Current user authentication
     * @return Success response
     */
    @PostMapping("/read-all")
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        notificationService.markAllAsRead(userId);

        log.info("Marked all notifications as read for user {}", userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Extract user ID from authentication.
     * TODO: Implement proper user ID extraction from JWT token.
     *
     * @param authentication Authentication object
     * @return User ID
     */
    private UUID getUserIdFromAuth(Authentication authentication) {
        // Placeholder: Extract from authentication principal
        // In real implementation, extract from JWT token claims
        String email = authentication.getName();

        // TODO: Look up user ID from email
        // For now, return a placeholder
        return UUID.randomUUID();
    }
}
