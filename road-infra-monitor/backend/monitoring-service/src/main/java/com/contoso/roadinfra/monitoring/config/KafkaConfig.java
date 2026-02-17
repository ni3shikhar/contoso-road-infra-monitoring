package com.contoso.roadinfra.monitoring.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic healthStatusChangesTopic() {
        return TopicBuilder.name("health-status-changes")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
