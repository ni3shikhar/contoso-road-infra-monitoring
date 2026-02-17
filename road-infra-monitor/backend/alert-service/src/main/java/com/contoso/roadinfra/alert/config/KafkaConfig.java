package com.contoso.roadinfra.alert.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic alertEventsTopic() {
        return TopicBuilder.name("alert-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
