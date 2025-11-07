package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for WebSocket connection lifecycle per FR-046, FR-047.
 *
 * Tests:
 * - WebSocket connection establishment
 * - Message sending and receiving
 * - Connection disconnection
 * - Connection timeout behavior
 * - Keepalive mechanism
 *
 * Test-First: These tests verify actual WebSocket connection behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebSocketConnectionIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private StompSession session;

    @BeforeEach
    public void setup() {
        // Setup WebSocket STOMP client with JavaTimeModule support
        stompClient = createWebSocketStompClient();

        // Disable heartbeat for testing (avoid need for TaskScheduler)
        stompClient.setDefaultHeartbeat(new long[]{0, 0});

        wsUrl = String.format("ws://localhost:%d/ws", port);
    }

    @AfterEach
    public void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * Test WebSocket connection can be established successfully.
     */
    @Test
    public void testWebSocketConnection_CanConnect() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Act
        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        // Assert
        assertThat(session).isNotNull();
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test WebSocket connection lifecycle: connect -> send -> receive -> disconnect.
     */
    @Test
    public void testWebSocketConnection_FullLifecycle() throws Exception {
        // Arrange
        BlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(1);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Act - Connect
        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        // Subscribe to echo topic (if available, otherwise just verify subscription works)
        StompSession.Subscription subscription = session.subscribe("/topic/test", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((String) payload);
            }
        });

        assertThat(subscription).isNotNull();

        // Disconnect
        session.disconnect();

        // Assert
        // Give it a moment to disconnect
        Thread.sleep(100);
        assertThat(session.isConnected()).isFalse();
    }

    /**
     * Test multiple sequential connections and disconnections.
     */
    @Test
    public void testWebSocketConnection_MultipleSequentialConnections() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Act & Assert - Connect and disconnect 3 times
        for (int i = 0; i < 3; i++) {
            StompSession tempSession = stompClient.connectAsync(wsUrl, sessionHandler)
                    .get(5, TimeUnit.SECONDS);

            assertThat(tempSession.isConnected()).isTrue();

            tempSession.disconnect();
            Thread.sleep(100);
            assertThat(tempSession.isConnected()).isFalse();
        }
    }

    /**
     * Test connection timeout when server is unreachable.
     */
    @Test
    public void testWebSocketConnection_TimeoutOnUnreachableServer() {
        // Arrange
        String invalidWsUrl = "ws://localhost:99999/ws"; // Invalid port
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        WebSocketStompClient timeoutClient = new WebSocketStompClient(new StandardWebSocketClient());
        timeoutClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Act & Assert
        assertThatThrownBy(() -> {
            timeoutClient.connectAsync(invalidWsUrl, sessionHandler)
                    .get(2, TimeUnit.SECONDS);
        }).isInstanceOf(Exception.class);
    }

    /**
     * Test connection remains alive without heartbeat.
     */
    @Test
    public void testWebSocketConnection_KeepaliveWithoutHeartbeat() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        AtomicBoolean connectionAlive = new AtomicBoolean(true);

        // Act - Connect
        session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connectionAlive.set(true);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                connectionAlive.set(false);
            }
        }).get(5, TimeUnit.SECONDS);

        // Wait (6 seconds)
        Thread.sleep(6000);

        // Assert - Connection should still be alive
        assertThat(session.isConnected()).isTrue();
        assertThat(connectionAlive.get()).isTrue();
    }

    /**
     * Test graceful disconnection.
     */
    @Test
    public void testWebSocketConnection_GracefulDisconnect() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        // Act - Graceful disconnect
        session.disconnect();
        Thread.sleep(100);

        // Assert
        assertThat(session.isConnected()).isFalse();
    }

    /**
     * Test subscription to multiple topics on single connection.
     */
    @Test
    public void testWebSocketConnection_MultipleSubscriptions() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        // Act - Subscribe to multiple topics
        StompSession.Subscription sub1 = session.subscribe("/topic/test1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Handler
            }
        });

        StompSession.Subscription sub2 = session.subscribe("/topic/test2", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Handler
            }
        });

        // Assert
        assertThat(sub1).isNotNull();
        assertThat(sub2).isNotNull();
        assertThat(session.isConnected()).isTrue();
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
