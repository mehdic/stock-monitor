package com.stockmonitor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket controller for notifications per FR-046, FR-047, FR-048.
 *
 * T117: Handles real-time notification delivery via WebSocket.
 *
 * Clients subscribe to: /user/{userId}/notifications
 * Server broadcasts NotificationDTO messages to specific users.
 *
 * Note: Actual broadcasting is handled by WebSocketNotificationService.
 * This controller provides subscription endpoints and handles client messages.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    /**
     * Handle notification subscription from client.
     *
     * Clients send a message to /app/notifications/subscribe to initiate subscription.
     * Spring automatically routes messages to /user/{userId}/notifications.
     *
     * @param userId User ID from client
     */
    @MessageMapping("/notifications/subscribe")
    public void subscribeToNotifications(@Payload String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            log.info("Client subscribed to notifications for user {}", userUuid);
            // Subscription is handled automatically by Spring WebSocket
            // Messages will be sent via WebSocketNotificationService.broadcastNotification()
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
        }
    }

    /**
     * Handle notification acknowledgment from client.
     *
     * Clients can send acknowledgments when they receive and display notifications.
     *
     * @param notificationId Notification ID
     */
    @MessageMapping("/notifications/ack")
    public void acknowledgeNotification(@Payload String notificationId) {
        try {
            UUID notifUuid = UUID.fromString(notificationId);
            log.debug("Client acknowledged notification {}", notifUuid);
            // Optional: Mark notification as delivered/displayed
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification ID format: {}", notificationId);
        }
    }
}
