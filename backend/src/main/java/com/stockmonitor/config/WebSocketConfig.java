package com.stockmonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

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

    /**
     * Configure message converters to support Java 8 date/time types.
     *
     * Registers JavaTimeModule to handle LocalDateTime, LocalDate, etc.
     * in WebSocket message serialization/deserialization.
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(createObjectMapper());
        converter.setContentTypeResolver(resolver);

        messageConverters.add(converter);
        return false; // Don't add default converters
    }

    /**
     * Create ObjectMapper with JavaTimeModule for Java 8 date/time support.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
