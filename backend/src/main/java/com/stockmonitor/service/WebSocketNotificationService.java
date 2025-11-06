package com.stockmonitor.service;

import com.stockmonitor.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * WebSocket notification broadcasting service per FR-046, FR-047.
 *
 * Broadcasts notifications to users via WebSocket connections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast notification to user via WebSocket.
     *
     * @param userId User ID
     * @param notification Notification DTO
     */
    public void broadcastNotification(UUID userId, NotificationDTO notification) {
        String destination = "/user/" + userId + "/notifications";

        try {
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Broadcast notification {} to user {} at {}", notification.getId(), userId, destination);
        } catch (Exception e) {
            log.error("Failed to broadcast notification to user {}: {}", userId, e.getMessage());
            throw e;
        }
    }
}
