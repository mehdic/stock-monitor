package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.controller.RunStatusWebSocketController;
import com.stockmonitor.dto.NotificationDTO;
import com.stockmonitor.dto.RunStatusUpdateDTO;
import com.stockmonitor.service.WebSocketNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WebSocket concurrency per FR-046, FR-047.
 *
 * Tests:
 * - Multiple concurrent WebSocket connections
 * - Each user receives only their own notifications
 * - Message isolation between connections
 * - Concurrent run status subscriptions
 * - No cross-contamination between users
 *
 * Test-First: These tests verify WebSocket behavior under concurrent load.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebSocketConcurrencyTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketNotificationService notificationService;

    @Autowired
    private RunStatusWebSocketController runStatusController;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private List<StompSession> activeSessions;

    @BeforeEach
    public void setup() {
        // Setup WebSocket STOMP client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setDefaultHeartbeat(new long[]{0, 0}); // Disable heartbeat for testing

        wsUrl = String.format("ws://localhost:%d/ws", port);
        activeSessions = new ArrayList<>();
    }

    @AfterEach
    public void cleanup() {
        for (StompSession session : activeSessions) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
        activeSessions.clear();
    }

    /**
     * Test 10 concurrent WebSocket connections can be established.
     */
    @Test
    public void testWebSocketConcurrency_TenConcurrentConnections() throws Exception {
        // Arrange
        int connectionCount = 10;

        // Act - Create 10 concurrent connections
        for (int i = 0; i < connectionCount; i++) {
            StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(10, TimeUnit.SECONDS);
            activeSessions.add(session);
        }

        // Assert
        assertThat(activeSessions).hasSize(connectionCount);

        // Verify all sessions are connected
        for (StompSession session : activeSessions) {
            assertThat(session.isConnected()).isTrue();
        }
    }

    /**
     * Test each user receives only their own notifications (no cross-contamination).
     */
    @Test
    public void testWebSocketConcurrency_MessageIsolationByUser() throws Exception {
        // Arrange - Create 5 users with separate connections
        int userCount = 5;
        Map<UUID, BlockingQueue<NotificationDTO>> userQueues = new ConcurrentHashMap<>();
        Map<UUID, StompSession> userSessions = new HashMap<>();

        for (int i = 0; i < userCount; i++) {
            UUID userId = UUID.randomUUID();
            BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);
            userQueues.put(userId, queue);

            StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(5, TimeUnit.SECONDS);
            activeSessions.add(session);
            userSessions.put(userId, session);

            // Subscribe to user-specific notifications
            session.subscribe("/user/" + userId + "/notifications", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return NotificationDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    queue.add((NotificationDTO) payload);
                }
            });
        }

        // Act - Send notification to each user
        List<UUID> userIds = new ArrayList<>(userQueues.keySet());
        for (int i = 0; i < userIds.size(); i++) {
            UUID userId = userIds.get(i);
            NotificationDTO notification = NotificationDTO.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .category("T_FINALIZED")
                    .priority("HIGH")
                    .title("Notification for User " + i)
                    .message("This is for user " + userId)
                    .actionLabel("View")
                    .actionUrl("/test")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationService.broadcastNotification(userId, notification);
        }

        // Wait for messages
        Thread.sleep(2000);

        // Assert - Each user received exactly 1 notification (their own)
        for (UUID userId : userIds) {
            BlockingQueue<NotificationDTO> queue = userQueues.get(userId);
            List<NotificationDTO> receivedNotifications = new ArrayList<>();

            NotificationDTO notification;
            while ((notification = queue.poll(100, TimeUnit.MILLISECONDS)) != null) {
                receivedNotifications.add(notification);
            }

            // Each user should receive exactly 1 notification
            assertThat(receivedNotifications).hasSize(1);

            // Verify it's their notification
            NotificationDTO received = receivedNotifications.get(0);
            assertThat(received.getUserId()).isEqualTo(userId);
            assertThat(received.getMessage()).contains(userId.toString());
        }
    }

    /**
     * Test concurrent run status subscriptions from multiple clients.
     */
    @Test
    public void testWebSocketConcurrency_ConcurrentRunStatusSubscriptions() throws Exception {
        // Arrange - Create 10 connections subscribing to same run
        int subscriberCount = 10;
        UUID runId = UUID.randomUUID();
        List<BlockingQueue<RunStatusUpdateDTO>> queues = new ArrayList<>();

        for (int i = 0; i < subscriberCount; i++) {
            BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(10);
            queues.add(queue);

            StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(5, TimeUnit.SECONDS);
            activeSessions.add(session);

            // Subscribe to run status
            session.subscribe("/topic/runs/" + runId + "/status", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return RunStatusUpdateDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    queue.add((RunStatusUpdateDTO) payload);
                }
            });
        }

        // Act - Broadcast single run status update
        runStatusController.sendStatusUpdate(runId, "RUNNING", 50, "Processing");

        // Wait for message delivery
        Thread.sleep(2000);

        // Assert - All subscribers received the same update
        int receiversCount = 0;
        for (BlockingQueue<RunStatusUpdateDTO> queue : queues) {
            RunStatusUpdateDTO update = queue.poll(100, TimeUnit.MILLISECONDS);
            if (update != null) {
                receiversCount++;
                assertThat(update.getRunId()).isEqualTo(runId);
                assertThat(update.getStatus()).isEqualTo("RUNNING");
                assertThat(update.getProgress()).isEqualTo(50);
            }
        }

        // At least most subscribers should have received the message
        assertThat(receiversCount).isGreaterThanOrEqualTo(subscriberCount - 1);
    }

    /**
     * Test multiple runs with different subscribers (no cross-contamination).
     */
    @Test
    public void testWebSocketConcurrency_MultipleRunsIsolation() throws Exception {
        // Arrange - Create 3 runs with separate subscriber sets
        int runCount = 3;
        Map<UUID, BlockingQueue<RunStatusUpdateDTO>> runQueues = new HashMap<>();

        for (int i = 0; i < runCount; i++) {
            UUID runId = UUID.randomUUID();
            BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(10);
            runQueues.put(runId, queue);

            StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(5, TimeUnit.SECONDS);
            activeSessions.add(session);

            // Subscribe to specific run
            session.subscribe("/topic/runs/" + runId + "/status", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return RunStatusUpdateDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    queue.add((RunStatusUpdateDTO) payload);
                }
            });
        }

        // Act - Send status updates to each run with different progress
        List<UUID> runIds = new ArrayList<>(runQueues.keySet());
        for (int i = 0; i < runIds.size(); i++) {
            UUID runId = runIds.get(i);
            int progress = (i + 1) * 25; // 25%, 50%, 75%
            runStatusController.sendStatusUpdate(runId, "RUNNING", progress, "Run " + i);
        }

        // Wait for messages
        Thread.sleep(2000);

        // Assert - Each run's subscriber received only their run's update
        for (int i = 0; i < runIds.size(); i++) {
            UUID runId = runIds.get(i);
            BlockingQueue<RunStatusUpdateDTO> queue = runQueues.get(runId);

            RunStatusUpdateDTO update = queue.poll(100, TimeUnit.MILLISECONDS);
            assertThat(update).isNotNull();
            assertThat(update.getRunId()).isEqualTo(runId);
            assertThat(update.getProgress()).isEqualTo((i + 1) * 25);

            // Should not receive additional messages
            assertThat(queue.poll(100, TimeUnit.MILLISECONDS)).isNull();
        }
    }

    /**
     * Test concurrent message sending (stress test).
     */
    @Test
    public void testWebSocketConcurrency_HighVolumeMessages() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(100);

        StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);
        activeSessions.add(session);

        session.subscribe("/user/" + userId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Act - Send 50 notifications rapidly
        int messageCount = 50;
        for (int i = 0; i < messageCount; i++) {
            NotificationDTO notification = NotificationDTO.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .category("T_FINALIZED")
                    .priority("HIGH")
                    .title("Notification " + i)
                    .message("Message number " + i)
                    .actionLabel("View")
                    .actionUrl("/test")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationService.broadcastNotification(userId, notification);
        }

        // Wait for messages to be delivered
        Thread.sleep(3000);

        // Collect all received messages
        List<NotificationDTO> receivedNotifications = new ArrayList<>();
        NotificationDTO notification;
        while ((notification = queue.poll(100, TimeUnit.MILLISECONDS)) != null) {
            receivedNotifications.add(notification);
        }

        // Assert - Most messages should have been delivered
        // Allow for some loss in high-volume scenario (>80% delivery is acceptable)
        assertThat(receivedNotifications.size()).isGreaterThan((int) (messageCount * 0.8));
    }

    /**
     * Test connection stability under concurrent load.
     */
    @Test
    public void testWebSocketConcurrency_ConnectionStability() throws Exception {
        // Arrange - Create 10 connections
        int connectionCount = 10;

        for (int i = 0; i < connectionCount; i++) {
            StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(5, TimeUnit.SECONDS);
            activeSessions.add(session);
        }

        // Act - Wait and check stability
        Thread.sleep(5000);

        // Assert - All connections should still be active
        int connectedCount = 0;
        for (StompSession session : activeSessions) {
            if (session.isConnected()) {
                connectedCount++;
            }
        }

        assertThat(connectedCount).isEqualTo(connectionCount);
    }

    /**
     * Test gradual connection and disconnection (churn).
     */
    @Test
    public void testWebSocketConcurrency_ConnectionChurn() throws Exception {
        // Act & Assert - Connect and disconnect 20 times
        int churnCycles = 20;

        for (int i = 0; i < churnCycles; i++) {
            // Connect
            StompSession session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(5, TimeUnit.SECONDS);
            assertThat(session.isConnected()).isTrue();

            // Subscribe
            session.subscribe("/topic/test", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // No-op
                }
            });

            // Small delay
            Thread.sleep(50);

            // Disconnect
            session.disconnect();
            Thread.sleep(50);
        }

        // If we got here without exceptions, test passed
        assertThat(true).isTrue();
    }

    /**
     * Helper class for STOMP session handling.
     */
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        private final CountDownLatch latch;

        public TestStompSessionHandler() {
            this.latch = null;
        }

        public TestStompSessionHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            exception.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            exception.printStackTrace();
        }
    }
}
