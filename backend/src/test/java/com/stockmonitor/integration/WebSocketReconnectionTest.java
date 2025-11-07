package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.NotificationDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WebSocket reconnection resilience per FR-046, FR-047.
 *
 * Tests:
 * - WebSocket reconnection after disconnection
 * - Connection recovery
 * - Subscription reestablishment after reconnection
 * - Message delivery after reconnection
 * - Graceful handling of connection failures
 *
 * Test-First: These tests verify WebSocket resilience and recovery behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebSocketReconnectionTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketNotificationService notificationService;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private StompSession session;

    @BeforeEach
    public void setup() {
        // Setup WebSocket STOMP client with JavaTimeModule support
        stompClient = createWebSocketStompClient();
        stompClient.setDefaultHeartbeat(new long[]{0, 0}); // Disable heartbeat for testing

        wsUrl = String.format("ws://localhost:%d/ws", port);
    }

    @AfterEach
    public void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * Test reconnection after graceful disconnect.
     */
    @Test
    public void testWebSocketReconnection_AfterGracefulDisconnect() throws Exception {
        // Arrange - Connect
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);
        assertThat(session.isConnected()).isTrue();

        // Act - Disconnect
        session.disconnect();
        Thread.sleep(500);
        assertThat(session.isConnected()).isFalse();

        // Reconnect
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

        // Assert
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test subscription reestablishment after reconnection.
     */
    @Test
    public void testWebSocketReconnection_SubscriptionReestablishment() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);

        // Initial connection and subscription
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Send notification - should be received
        NotificationDTO notification1 = createTestNotification(userId, "Notification 1");
        notificationService.broadcastNotification(userId, notification1);

        NotificationDTO received1 = queue.poll(3, TimeUnit.SECONDS);
        assertThat(received1).isNotNull();
        assertThat(received1.getTitle()).isEqualTo("Notification 1");

        // Act - Disconnect
        session.disconnect();
        Thread.sleep(500);

        // Reconnect and resubscribe
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Send notification after reconnection
        NotificationDTO notification2 = createTestNotification(userId, "Notification 2");
        notificationService.broadcastNotification(userId, notification2);

        NotificationDTO received2 = queue.poll(3, TimeUnit.SECONDS);

        // Assert - Notification received after reconnection
        assertThat(received2).isNotNull();
        assertThat(received2.getTitle()).isEqualTo("Notification 2");
    }

    /**
     * Test multiple reconnection cycles.
     */
    @Test
    public void testWebSocketReconnection_MultipleCycles() throws Exception {
        // Act & Assert - 5 cycles of connect/disconnect/reconnect
        int reconnectionCycles = 5;

        for (int i = 0; i < reconnectionCycles; i++) {
            // Connect
            session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
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

            // Disconnect
            session.disconnect();
            Thread.sleep(200);
            assertThat(session.isConnected()).isFalse();
        }

        // Final reconnection
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test message delivery after reconnection.
     */
    @Test
    public void testWebSocketReconnection_MessageDeliveryAfterReconnect() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);

        // Connect and subscribe
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Act - Simulate disconnect/reconnect
        session.disconnect();
        Thread.sleep(500);

        // Send messages while disconnected (these will be lost)
        notificationService.broadcastNotification(userId, createTestNotification(userId, "Lost Message"));

        // Reconnect
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Send message after reconnection
        notificationService.broadcastNotification(userId, createTestNotification(userId, "After Reconnect"));

        NotificationDTO received = queue.poll(3, TimeUnit.SECONDS);

        // Assert - Message sent after reconnection is received
        assertThat(received).isNotNull();
        assertThat(received.getTitle()).isEqualTo("After Reconnect");
    }

    /**
     * Test connection recovery with automatic retry.
     */
    @Test
    public void testWebSocketReconnection_AutomaticRetry() throws Exception {
        // Arrange
        AtomicBoolean connected = new AtomicBoolean(false);
        int maxRetries = 3;
        int retryCount = 0;

        // Act - Try to connect with retries
        while (retryCount < maxRetries && !connected.get()) {
            try {
                session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                        .get(5, TimeUnit.SECONDS);
                connected.set(session.isConnected());
            } catch (Exception e) {
                retryCount++;
                Thread.sleep(1000); // Wait before retry
            }
        }

        // Assert
        assertThat(connected.get()).isTrue();
        assertThat(session).isNotNull();
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test graceful handling of connection failure during operation.
     */
    @Test
    public void testWebSocketReconnection_GracefulFailureHandling() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);
        List<NotificationDTO> receivedMessages = new ArrayList<>();

        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Act - Send messages, disconnect mid-stream, reconnect, continue
        notificationService.broadcastNotification(userId, createTestNotification(userId, "Message 1"));
        Thread.sleep(200);

        NotificationDTO msg1 = queue.poll(2, TimeUnit.SECONDS);
        if (msg1 != null) receivedMessages.add(msg1);

        // Disconnect
        session.disconnect();
        Thread.sleep(500);

        // Messages sent during disconnection are lost
        notificationService.broadcastNotification(userId, createTestNotification(userId, "Lost Message"));

        // Reconnect
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Continue sending
        notificationService.broadcastNotification(userId, createTestNotification(userId, "Message 3"));
        Thread.sleep(200);

        NotificationDTO msg3 = queue.poll(2, TimeUnit.SECONDS);
        if (msg3 != null) receivedMessages.add(msg3);

        // Assert - Received messages before and after disconnection
        assertThat(receivedMessages).hasSizeGreaterThanOrEqualTo(2);
        assertThat(receivedMessages.get(0).getTitle()).isEqualTo("Message 1");
        assertThat(receivedMessages.get(receivedMessages.size() - 1).getTitle()).isEqualTo("Message 3");
    }

    /**
     * Test rapid connect/disconnect cycles (stress test).
     */
    @Test
    public void testWebSocketReconnection_RapidCycles() throws Exception {
        // Act - Rapid connect/disconnect 20 times
        int cycles = 20;

        for (int i = 0; i < cycles; i++) {
            session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                    .get(5, TimeUnit.SECONDS);
            assertThat(session.isConnected()).isTrue();

            session.disconnect();
            // Minimal delay
            Thread.sleep(10);
        }

        // Final connect to verify system still stable
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

        // Assert
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test connection timeout and recovery.
     */
    @Test
    public void testWebSocketReconnection_TimeoutAndRecovery() throws Exception {
        // Arrange - Use default client
        // Act - Connect
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        // Wait some time
        Thread.sleep(3000);

        // Assert - Connection should still be alive
        assertThat(session.isConnected()).isTrue();

        // Disconnect
        session.disconnect();
        Thread.sleep(500);

        // Reconnect
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test subscription persistence check (subscriptions don't persist after disconnect).
     */
    @Test
    public void testWebSocketReconnection_SubscriptionDoesNotPersist() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);

        // Connect and subscribe
        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

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

        // Verify subscription works
        notificationService.broadcastNotification(userId, createTestNotification(userId, "Before Disconnect"));
        assertThat(queue.poll(2, TimeUnit.SECONDS)).isNotNull();

        // Act - Disconnect and reconnect WITHOUT resubscribing
        session.disconnect();
        Thread.sleep(500);

        session = stompClient.connectAsync(wsUrl, new TestStompSessionHandler())
                .get(5, TimeUnit.SECONDS);

        // Send message WITHOUT resubscribing
        notificationService.broadcastNotification(userId, createTestNotification(userId, "After Reconnect"));

        // Assert - Message should NOT be received (subscription not restored)
        NotificationDTO notReceived = queue.poll(2, TimeUnit.SECONDS);
        assertThat(notReceived).isNull();
    }

    /**
     * Helper method to create test notification.
     */
    private NotificationDTO createTestNotification(UUID userId, String title) {
        return NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .category("T_FINALIZED")
                .priority("HIGH")
                .title(title)
                .message("Test message: " + title)
                .actionLabel("View")
                .actionUrl("/test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Helper class for STOMP session handling.
     */
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // Connection successful
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
