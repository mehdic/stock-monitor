package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.controller.RunStatusWebSocketController;
import com.stockmonitor.dto.RunStatusUpdateDTO;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.RecommendationRunRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for run status WebSocket updates per FR-046, FR-047.
 *
 * Tests:
 * - Subscribe to /topic/runs/{runId}/status
 * - Receive real-time run status updates
 * - Verify status transitions: QUEUED → RUNNING → FINALIZED
 * - Verify progress percentage updates
 * - Verify message structure and content
 *
 * Test-First: These tests verify actual WebSocket run status update behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RunStatusWebSocketIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RecommendationRunRepository runRepository;

    @Autowired
    private RunStatusWebSocketController runStatusController;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private StompSession session;
    private UUID testRunId;

    @BeforeEach
    public void setup() {
        // Setup WebSocket STOMP client with JavaTimeModule support
        stompClient = createWebSocketStompClient();

        wsUrl = String.format("ws://localhost:%d/ws", port);

        // Create test run
        RecommendationRun run = RecommendationRun.builder()
                .userId(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .universeId(UUID.randomUUID())
                .constraintSetId(UUID.randomUUID())
                .runType("SCHEDULED")
                .status("SCHEDULED")
                .scheduledDate(LocalDate.now())
                .dataFreshnessCheckPassed(true)
                .build();

        run = runRepository.save(run);
        testRunId = run.getId();
    }

    @AfterEach
    public void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        runRepository.deleteAll();
    }

    /**
     * Test subscribing to run status updates.
     */
    @Test
    public void testRunStatusWebSocket_CanSubscribe() throws Exception {
        // Arrange
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Act
        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        // Subscribe to run status updates
        BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(1);
        session.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Assert
        assertThat(session.isConnected()).isTrue();
    }

    /**
     * Test receiving run status update via WebSocket.
     */
    @Test
    public void testRunStatusWebSocket_ReceivesStatusUpdate() throws Exception {
        // Arrange
        BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Act - Broadcast status update
        runStatusController.sendStatusUpdate(testRunId, "RUNNING", 50, "Processing data");

        // Wait for message
        RunStatusUpdateDTO update = queue.poll(3, TimeUnit.SECONDS);

        // Assert
        assertThat(update).isNotNull();
        assertThat(update.getRunId()).isEqualTo(testRunId);
        assertThat(update.getStatus()).isEqualTo("RUNNING");
        assertThat(update.getProgress()).isEqualTo(50);
        assertThat(update.getStage()).isEqualTo("Processing data");
        assertThat(update.getTimestamp()).isNotNull();
    }

    /**
     * Test run status transitions: QUEUED → RUNNING → FINALIZED.
     */
    @Test
    public void testRunStatusWebSocket_StatusTransitions() throws Exception {
        // Arrange
        BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(10);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Act - Send status transitions
        runStatusController.sendStatusUpdate(testRunId, "QUEUED", 0, "Queued for execution");
        Thread.sleep(100); // Small delay between updates

        runStatusController.sendStatusUpdate(testRunId, "RUNNING", 25, "Data loading");
        Thread.sleep(100);

        runStatusController.sendStatusUpdate(testRunId, "RUNNING", 50, "Processing");
        Thread.sleep(100);

        runStatusController.sendStatusUpdate(testRunId, "RUNNING", 75, "Finalizing");
        Thread.sleep(100);

        runStatusController.sendStatusUpdate(testRunId, "FINALIZED", 100, "Complete");

        // Wait for all messages
        List<RunStatusUpdateDTO> updates = new ArrayList<>();
        RunStatusUpdateDTO update;
        while ((update = queue.poll(2, TimeUnit.SECONDS)) != null) {
            updates.add(update);
        }

        // Assert
        assertThat(updates).hasSizeGreaterThanOrEqualTo(5);

        // Verify first update
        RunStatusUpdateDTO firstUpdate = updates.get(0);
        assertThat(firstUpdate.getStatus()).isEqualTo("QUEUED");
        assertThat(firstUpdate.getProgress()).isEqualTo(0);

        // Verify last update
        RunStatusUpdateDTO lastUpdate = updates.get(updates.size() - 1);
        assertThat(lastUpdate.getStatus()).isEqualTo("FINALIZED");
        assertThat(lastUpdate.getProgress()).isEqualTo(100);

        // Verify all updates have correct runId
        updates.forEach(u -> assertThat(u.getRunId()).isEqualTo(testRunId));
    }

    /**
     * Test progress percentage updates from 0 to 100.
     */
    @Test
    public void testRunStatusWebSocket_ProgressUpdates() throws Exception {
        // Arrange
        BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(10);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Act - Send progress updates
        for (int progress = 0; progress <= 100; progress += 20) {
            runStatusController.sendStatusUpdate(testRunId, "RUNNING", progress, "Progress: " + progress + "%");
            Thread.sleep(50);
        }

        // Wait for messages
        List<RunStatusUpdateDTO> updates = new ArrayList<>();
        RunStatusUpdateDTO update;
        while ((update = queue.poll(2, TimeUnit.SECONDS)) != null) {
            updates.add(update);
        }

        // Assert
        assertThat(updates).hasSizeGreaterThanOrEqualTo(6); // 0, 20, 40, 60, 80, 100

        // Verify progress values are in range
        updates.forEach(u -> {
            assertThat(u.getProgress()).isBetween(0, 100);
        });

        // Verify progress increases monotonically (or stays same)
        for (int i = 1; i < updates.size(); i++) {
            assertThat(updates.get(i).getProgress())
                    .isGreaterThanOrEqualTo(updates.get(i - 1).getProgress());
        }
    }

    /**
     * Test error status update with error message.
     */
    @Test
    public void testRunStatusWebSocket_ErrorUpdate() throws Exception {
        // Arrange
        BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Act - Send error update
        String errorMessage = "Data validation failed: Missing required field 'price'";
        runStatusController.sendErrorUpdate(testRunId, errorMessage);

        // Wait for message
        RunStatusUpdateDTO update = queue.poll(3, TimeUnit.SECONDS);

        // Assert
        assertThat(update).isNotNull();
        assertThat(update.getRunId()).isEqualTo(testRunId);
        assertThat(update.getStatus()).isEqualTo("FAILED");
        assertThat(update.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(update.getTimestamp()).isNotNull();
    }

    /**
     * Test multiple subscribers receive same updates.
     */
    @Test
    public void testRunStatusWebSocket_MultipleSubscribers() throws Exception {
        // Arrange
        BlockingQueue<RunStatusUpdateDTO> queue1 = new ArrayBlockingQueue<>(5);
        BlockingQueue<RunStatusUpdateDTO> queue2 = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        // Connect two sessions
        StompSession session1 = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);
        StompSession session2 = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        // Both subscribe to same run
        session1.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue1.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        session2.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue2.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Act - Broadcast update
        runStatusController.sendStatusUpdate(testRunId, "RUNNING", 50, "Processing");

        // Wait for messages
        RunStatusUpdateDTO update1 = queue1.poll(3, TimeUnit.SECONDS);
        RunStatusUpdateDTO update2 = queue2.poll(3, TimeUnit.SECONDS);

        // Assert - Both subscribers received the same update
        assertThat(update1).isNotNull();
        assertThat(update2).isNotNull();
        assertThat(update1.getRunId()).isEqualTo(update2.getRunId());
        assertThat(update1.getStatus()).isEqualTo(update2.getStatus());
        assertThat(update1.getProgress()).isEqualTo(update2.getProgress());

        // Cleanup
        session1.disconnect();
        session2.disconnect();
    }

    /**
     * Test message structure contains all required fields.
     */
    @Test
    public void testRunStatusWebSocket_MessageStructure() throws Exception {
        // Arrange
        BlockingQueue<RunStatusUpdateDTO> queue = new ArrayBlockingQueue<>(5);
        StompSessionHandler sessionHandler = new TestStompSessionHandler();

        session = stompClient.connectAsync(wsUrl, sessionHandler)
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/runs/" + testRunId + "/status", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RunStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((RunStatusUpdateDTO) payload);
            }
        });

        // Wait for subscription to be fully established
        Thread.sleep(500);

        // Act
        runStatusController.sendStatusUpdate(testRunId, "RUNNING", 75, "Finalizing recommendations");

        RunStatusUpdateDTO update = queue.poll(3, TimeUnit.SECONDS);

        // Assert - Verify all required fields present
        assertThat(update).isNotNull();
        assertThat(update.getRunId()).isNotNull();
        assertThat(update.getStatus()).isNotNull();
        assertThat(update.getProgress()).isNotNull();
        assertThat(update.getStage()).isNotNull();
        assertThat(update.getTimestamp()).isNotNull();

        // Verify status is valid
        assertThat(update.getStatus()).isIn("QUEUED", "RUNNING", "FINALIZED", "FAILED");

        // Verify progress is in valid range
        assertThat(update.getProgress()).isBetween(0, 100);
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
