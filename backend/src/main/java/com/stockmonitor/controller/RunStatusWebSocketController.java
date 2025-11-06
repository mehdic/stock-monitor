package com.stockmonitor.controller;

import com.stockmonitor.dto.RunStatusUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket controller for run status updates per FR-046, FR-047.
 *
 * T116: Handles real-time run progress updates via WebSocket.
 *
 * Clients subscribe to: /topic/runs/{runId}/status
 * Server broadcasts RunStatusUpdateDTO messages with progress updates.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RunStatusWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast run status update to all subscribers.
     *
     * @param runId Run ID
     * @param update Status update DTO
     */
    public void broadcastRunStatus(UUID runId, RunStatusUpdateDTO update) {
        String destination = "/topic/runs/" + runId + "/status";

        try {
            messagingTemplate.convertAndSend(destination, update);
            log.debug("Broadcast run status update to {}: status={}, progress={}%",
                    destination, update.getStatus(), update.getProgress());
        } catch (Exception e) {
            log.error("Failed to broadcast run status update: {}", e.getMessage());
        }
    }

    /**
     * Subscribe to run status updates.
     *
     * This method is invoked when a client subscribes to /topic/runs/{runId}/status.
     * Spring automatically handles the subscription.
     *
     * @param runId Run ID
     */
    @MessageMapping("/runs/{runId}/status/subscribe")
    public void subscribeToRunStatus(@DestinationVariable UUID runId) {
        log.info("Client subscribed to run status updates for run {}", runId);

        // Send initial status update to acknowledge subscription
        RunStatusUpdateDTO initialUpdate = RunStatusUpdateDTO.builder()
                .runId(runId)
                .status("SUBSCRIBED")
                .progress(0)
                .stage("Subscribed to status updates")
                .timestamp(LocalDateTime.now())
                .build();

        broadcastRunStatus(runId, initialUpdate);
    }

    /**
     * Helper method to create and broadcast run status update.
     *
     * @param runId Run ID
     * @param status Run status
     * @param progress Progress percentage (0-100)
     * @param stage Current stage description
     */
    public void sendStatusUpdate(UUID runId, String status, int progress, String stage) {
        RunStatusUpdateDTO update = RunStatusUpdateDTO.builder()
                .runId(runId)
                .status(status)
                .progress(progress)
                .stage(stage)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastRunStatus(runId, update);
    }

    /**
     * Send status update with error message.
     *
     * @param runId Run ID
     * @param errorMessage Error message
     */
    public void sendErrorUpdate(UUID runId, String errorMessage) {
        RunStatusUpdateDTO update = RunStatusUpdateDTO.builder()
                .runId(runId)
                .status("FAILED")
                .progress(0)
                .stage("Run failed")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastRunStatus(runId, update);
    }
}
