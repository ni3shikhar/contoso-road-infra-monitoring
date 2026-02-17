package com.contoso.roadinfra.sensor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time sensor data streaming.
 * 
 * Available topics:
 * - /topic/sensor-readings/{sensorId} - Live readings for a specific sensor
 * - /topic/sensor-readings/all - All live sensor readings
 * - /topic/sensor-alerts - Sensor alerts broadcast
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for subscriptions
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages from client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint at /ws-sensors
        registry.addEndpoint("/ws-sensors")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:8080"
                )
                .withSockJS();

        // Raw WebSocket without SockJS fallback
        registry.addEndpoint("/ws-sensors")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:8080"
                );
    }
}
