package com.stockmonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time updates per FR-046, FR-047.
 *
 * T115: Configures Spring WebSocket with STOMP for:
 * - Run status updates (/topic/runs/{runId}/status)
 * - User notifications (/user/{userId}/notifications)
 *
 * Uses STOMP over WebSocket protocol for bidirectional communication.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for pub/sub messaging.
     *
     * - /topic: Broadcast to all subscribers
     * - /user: Point-to-point messaging to specific users
     * - /app: Application destination prefix for messages from clients
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic and /user destinations
        registry.enableSimpleBroker("/topic", "/user");

        // Set application destination prefix for messages from clients
        registry.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix (default is /user)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     *
     * Clients connect to /ws endpoint and upgrade to WebSocket protocol.
     * SockJS fallback enabled for browsers without WebSocket support.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain WebSocket endpoint (for native WebSocket clients and testing)
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*"); // Allow all origins for testing

        // SockJS endpoint (for browsers without WebSocket support)
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173") // Frontend origins
                .withSockJS(); // Enable SockJS fallback
    }
}
