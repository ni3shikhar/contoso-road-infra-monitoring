package com.contoso.roadinfra.sensor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for sensor service.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic sensorEventsTopic() {
        return TopicBuilder.name("sensor-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sensorTelemetryTopic() {
        return TopicBuilder.name("sensor-telemetry")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sensorAlertsTopic() {
        return TopicBuilder.name("sensor-alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sensorStatusChangesTopic() {
        return TopicBuilder.name("sensor-status-changes")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sensorAnomaliesTopic() {
        return TopicBuilder.name("sensor-anomalies")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
