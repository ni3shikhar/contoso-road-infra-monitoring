package com.contoso.roadinfra.asset.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for asset-service.
 * Creates topics for asset events.
 */
@Configuration
public class KafkaConfig {

    public static final String TOPIC_ASSET_HEALTH_CHANGES = "asset-health-changes";
    public static final String TOPIC_CONSTRUCTION_PROGRESS = "construction-progress";
    public static final String TOPIC_ASSET_EVENTS = "asset-events";
    public static final String TOPIC_MILESTONE_EVENTS = "milestone-events";

    @Bean
    public NewTopic assetHealthChangesTopic() {
        return TopicBuilder.name(TOPIC_ASSET_HEALTH_CHANGES)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic constructionProgressTopic() {
        return TopicBuilder.name(TOPIC_CONSTRUCTION_PROGRESS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic assetEventsTopic() {
        return TopicBuilder.name(TOPIC_ASSET_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic milestoneEventsTopic() {
        return TopicBuilder.name(TOPIC_MILESTONE_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
