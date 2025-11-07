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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for notification WebSocket delivery per FR-045, FR-046, FR-047, FR-048.
 *
 * Tests:
 * - Subscribe to /user/{userId}/notifications
 * - Receive real-time notifications
 * - Verify notification categories and priorities
 * - Verify notification read/unread status
 * - Test notification filtering by user
 *
 * Test-First: These tests verify actual WebSocket notification delivery behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NotificationWebSocketIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketNotificationService notificationService;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private StompSession session;
    private UUID testUserId;

    @BeforeEach
    public void setup() {
        // Setup WebSocket STOMP client with JavaTimeModule support
        stompClient = createWebSocketStompClient();

        wsUrl = String.format("ws://localhost:%d/ws", port);
        testUserId = UUID.randomUUID();
    }

    @AfterEach
    public void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * Test subscribing to user notifications.
     */
    @Test
    public void testNotificationWebSocket_CanSubscribe() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Act
        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        // Subscribe to user notifications
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(1);
        session.subscribe("/user/" + testUserId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Assert
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test receiving notification via WebSocket.
     */
    @Test
    public void testNotificationWebSocket_ReceivesNotification() throws Exception {
        // Arrange
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/user/" + testUserId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Act - Broadcast notification
        NotificationDTO notification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .category("T_FINALIZED")
                .priority("HIGH")
                .title("Recommendations Ready")
                .message("Your month-end recommendations are ready for review")
                .actionLabel("View Recommendations")
                .actionUrl("/recommendations")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.broadcastNotification(testUserId, notification);

        // Wait for message
        NotificationDTO received = queue.poll(3, TimeUnit.SECONDS);

        // Assert
        assertThat(received).isNotNull();
        assertThat(received.getId()).isEqualTo(notification.getId());
        assertThat(received.getUserId()).isEqualTo(testUserId);
        assertThat(received.getCategory()).isEqualTo("T_FINALIZED");
        assertThat(received.getPriority()).isEqualTo("HIGH");
        assertThat(received.getTitle()).isEqualTo("Recommendations Ready");
        assertThat(received.getMessage()).isEqualTo("Your month-end recommendations are ready for review");
    }

    /**
     * Test notification categories per FR-045.
     */
    @Test
    public void testNotificationWebSocket_AllCategories() throws Exception {
        // Arrange
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/user/" + testUserId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Act - Send notifications for all categories
        String[] categories = {
                "T-3_PRECOMPUTE",
                "T-1_STAGED",
                "T_FINALIZED",
                "DATA_STALE",
                "RUN_FAILED",
                "CONSTRAINT_VIOLATED"
        };

        for (String category : categories) {
            NotificationDTO notification = NotificationDTO.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .category(category)
                    .priority("MEDIUM")
                    .title("Test Notification: " + category)
                    .message("Testing category: " + category)
                    .actionLabel("View")
                    .actionUrl("/test")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationService.broadcastNotification(testUserId, notification);
            Thread.sleep(50); // Small delay between notifications
        }

        // Wait for all messages
        List<NotificationDTO> notifications = new ArrayList<>();
        NotificationDTO notification;
        while ((notification = queue.poll(2, TimeUnit.SECONDS)) != null) {
            notifications.add(notification);
        }

        // Assert
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(6);

        // Verify all categories received
        List<String> receivedCategories = notifications.stream()
                .map(NotificationDTO::getCategory)
                .toList();

        for (String category : categories) {
            assertThat(receivedCategories).contains(category);
        }
    }

    /**
     * Test notification priorities: HIGH, MEDIUM, LOW.
     */
    @Test
    public void testNotificationWebSocket_AllPriorities() throws Exception {
        // Arrange
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(10);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/user/" + testUserId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Act - Send notifications with different priorities
        String[] priorities = {"HIGH", "MEDIUM", "LOW"};

        for (String priority : priorities) {
            NotificationDTO notification = NotificationDTO.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .category("T_FINALIZED")
                    .priority(priority)
                    .title("Test Priority: " + priority)
                    .message("Testing priority: " + priority)
                    .actionLabel("View")
                    .actionUrl("/test")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationService.broadcastNotification(testUserId, notification);
            Thread.sleep(50);
        }

        // Wait for all messages
        List<NotificationDTO> notifications = new ArrayList<>();
        NotificationDTO notification;
        while ((notification = queue.poll(2, TimeUnit.SECONDS)) != null) {
            notifications.add(notification);
        }

        // Assert
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(3);

        // Verify all priorities received
        List<String> receivedPriorities = notifications.stream()
                .map(NotificationDTO::getPriority)
                .toList();

        assertThat(receivedPriorities).containsAll(List.of("HIGH", "MEDIUM", "LOW"));
    }

    /**
     * Test notification read/unread status tracking per FR-048.
     */
    @Test
    public void testNotificationWebSocket_ReadStatus() throws Exception {
        // Arrange
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/user/" + testUserId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Act - Send unread notification
        NotificationDTO unreadNotification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .category("T_FINALIZED")
                .priority("HIGH")
                .title("Unread Notification")
                .message("This notification is unread")
                .actionLabel("View")
                .actionUrl("/test")
                .isRead(false)
                .readAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.broadcastNotification(testUserId, unreadNotification);

        NotificationDTO received1 = queue.poll(3, TimeUnit.SECONDS);

        // Assert - Unread notification
        assertThat(received1).isNotNull();
        assertThat(received1.getIsRead()).isFalse();
        assertThat(received1.getReadAt()).isNull();

        // Act - Send read notification
        NotificationDTO readNotification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .category("T-1_STAGED")
                .priority("MEDIUM")
                .title("Read Notification")
                .message("This notification is read")
                .actionLabel("View")
                .actionUrl("/test")
                .isRead(true)
                .readAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        notificationService.broadcastNotification(testUserId, readNotification);

        NotificationDTO received2 = queue.poll(3, TimeUnit.SECONDS);

        // Assert - Read notification
        assertThat(received2).isNotNull();
        assertThat(received2.getIsRead()).isTrue();
        assertThat(received2.getReadAt()).isNotNull();
    }

    /**
     * Test notifications are isolated by user ID.
     */
    @Test
    public void testNotificationWebSocket_UserIsolation() throws Exception {
        // Arrange
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        BlockingQueue<NotificationDTO> user1Queue = new ArrayBlockingQueue<>(5);
        BlockingQueue<NotificationDTO> user2Queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Connect two sessions for different users
        StompSession user1Session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);
        StompSession user2Session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        // User 1 subscribes to their notifications
        user1Session.subscribe("/user/" + user1Id + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                user1Queue.add((NotificationDTO) payload);
            }
        });

        // User 2 subscribes to their notifications
        user2Session.subscribe("/user/" + user2Id + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                user2Queue.add((NotificationDTO) payload);
            }
        });

        // Act - Send notification to User 1
        NotificationDTO user1Notification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(user1Id)
                .category("T_FINALIZED")
                .priority("HIGH")
                .title("User 1 Notification")
                .message("This is for User 1")
                .actionLabel("View")
                .actionUrl("/test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.broadcastNotification(user1Id, user1Notification);

        // Act - Send notification to User 2
        NotificationDTO user2Notification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(user2Id)
                .category("DATA_STALE")
                .priority("HIGH")
                .title("User 2 Notification")
                .message("This is for User 2")
                .actionLabel("View")
                .actionUrl("/test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.broadcastNotification(user2Id, user2Notification);

        // Wait for messages
        NotificationDTO user1Received = user1Queue.poll(3, TimeUnit.SECONDS);
        NotificationDTO user2Received = user2Queue.poll(3, TimeUnit.SECONDS);

        // Assert - Each user received only their notification
        assertThat(user1Received).isNotNull();
        assertThat(user1Received.getUserId()).isEqualTo(user1Id);
        assertThat(user1Received.getTitle()).isEqualTo("User 1 Notification");

        assertThat(user2Received).isNotNull();
        assertThat(user2Received.getUserId()).isEqualTo(user2Id);
        assertThat(user2Received.getTitle()).isEqualTo("User 2 Notification");

        // Assert - Users did NOT receive each other's notifications
        assertThat(user1Queue.poll(1, TimeUnit.SECONDS)).isNull();
        assertThat(user2Queue.poll(1, TimeUnit.SECONDS)).isNull();

        // Cleanup
        user1Session.disconnect();
        user2Session.disconnect();
    }

    /**
     * Test notification message structure contains all required fields.
     */
    @Test
    public void testNotificationWebSocket_MessageStructure() throws Exception {
        // Arrange
        BlockingQueue<NotificationDTO> queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/user/" + testUserId + "/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationDTO) payload);
            }
        });

        // Act
        NotificationDTO notification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .category("T_FINALIZED")
                .priority("HIGH")
                .title("Test Notification")
                .message("Test message")
                .actionLabel("View")
                .actionUrl("/test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.broadcastNotification(testUserId, notification);

        NotificationDTO received = queue.poll(3, TimeUnit.SECONDS);

        // Assert - Verify all required fields present per FR-045
        assertThat(received).isNotNull();
        assertThat(received.getId()).isNotNull();
        assertThat(received.getUserId()).isNotNull();
        assertThat(received.getCategory()).isNotNull();
        assertThat(received.getPriority()).isNotNull();
        assertThat(received.getTitle()).isNotNull();
        assertThat(received.getMessage()).isNotNull();
        assertThat(received.getActionLabel()).isNotNull();
        assertThat(received.getActionUrl()).isNotNull();
        assertThat(received.getIsRead()).isNotNull();
        assertThat(received.getCreatedAt()).isNotNull();

        // Verify valid values
        assertThat(received.getCategory()).isIn(
                "T-3_PRECOMPUTE", "T-1_STAGED", "T_FINALIZED",
                "DATA_STALE", "RUN_FAILED", "CONSTRAINT_VIOLATED"
        );
        assertThat(received.getPriority()).isIn("HIGH", "MEDIUM", "LOW");
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
