package com.contoso.roadinfra.asset.event;

import com.contoso.roadinfra.asset.config.KafkaConfig;
import com.contoso.roadinfra.asset.dto.AssetResponse;
import com.contoso.roadinfra.asset.dto.MilestoneResponse;
import com.contoso.roadinfra.asset.entity.Asset;
import com.contoso.roadinfra.asset.entity.ConstructionMilestone;
import com.contoso.roadinfra.asset.mapper.AssetMapper;
import com.contoso.roadinfra.asset.mapper.MilestoneMapper;
import com.contoso.roadinfra.common.constants.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes asset-related events to Kafka topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AssetMapper assetMapper;
    private final MilestoneMapper milestoneMapper;

    /**
     * Publish health status change event.
     */
    @Async
    public void publishHealthStatusChange(Asset asset, HealthStatus oldStatus, String reason) {
        log.debug("Publishing health status change for asset {}: {} -> {}",
                asset.getAssetCode(), oldStatus, asset.getHealthStatus());

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "HEALTH_STATUS_CHANGED");
        event.put("assetId", asset.getId());
        event.put("assetCode", asset.getAssetCode());
        event.put("assetName", asset.getName());
        event.put("assetType", asset.getAssetType());
        event.put("oldStatus", oldStatus);
        event.put("newStatus", asset.getHealthStatus());
        event.put("reason", reason);
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_ASSET_HEALTH_CHANGES, asset.getId().toString(), event);
            log.info("Published health status change event for asset {}", asset.getAssetCode());
        } catch (Exception e) {
            log.error("Failed to publish health status change event: {}", e.getMessage());
        }
    }

    /**
     * Publish construction progress update event.
     */
    @Async
    public void publishProgressUpdate(Asset asset, Double oldProgress, Double newProgress, String notes) {
        log.debug("Publishing progress update for asset {}: {}% -> {}%",
                asset.getAssetCode(), oldProgress, newProgress);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PROGRESS_UPDATED");
        event.put("assetId", asset.getId());
        event.put("assetCode", asset.getAssetCode());
        event.put("assetName", asset.getName());
        event.put("assetType", asset.getAssetType());
        event.put("oldProgress", oldProgress);
        event.put("newProgress", newProgress);
        event.put("progressDelta", newProgress - oldProgress);
        event.put("notes", notes);
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_CONSTRUCTION_PROGRESS, asset.getId().toString(), event);
            log.info("Published progress update event for asset {}", asset.getAssetCode());
        } catch (Exception e) {
            log.error("Failed to publish progress update event: {}", e.getMessage());
        }
    }

    /**
     * Publish asset created event.
     */
    @Async
    public void publishAssetCreated(Asset asset) {
        log.debug("Publishing asset created event for {}", asset.getAssetCode());

        AssetResponse response = assetMapper.toResponse(asset);
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ASSET_CREATED");
        event.put("asset", response);
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_ASSET_EVENTS, asset.getId().toString(), event);
            log.info("Published asset created event for {}", asset.getAssetCode());
        } catch (Exception e) {
            log.error("Failed to publish asset created event: {}", e.getMessage());
        }
    }

    /**
     * Publish asset updated event.
     */
    @Async
    public void publishAssetUpdated(Asset asset) {
        log.debug("Publishing asset updated event for {}", asset.getAssetCode());

        AssetResponse response = assetMapper.toResponse(asset);
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ASSET_UPDATED");
        event.put("asset", response);
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_ASSET_EVENTS, asset.getId().toString(), event);
            log.info("Published asset updated event for {}", asset.getAssetCode());
        } catch (Exception e) {
            log.error("Failed to publish asset updated event: {}", e.getMessage());
        }
    }

    /**
     * Publish asset deleted event.
     */
    @Async
    public void publishAssetDeleted(Asset asset) {
        log.debug("Publishing asset deleted event for {}", asset.getAssetCode());

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ASSET_DELETED");
        event.put("assetId", asset.getId());
        event.put("assetCode", asset.getAssetCode());
        event.put("assetName", asset.getName());
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_ASSET_EVENTS, asset.getId().toString(), event);
            log.info("Published asset deleted event for {}", asset.getAssetCode());
        } catch (Exception e) {
            log.error("Failed to publish asset deleted event: {}", e.getMessage());
        }
    }

    /**
     * Publish milestone created event.
     */
    @Async
    public void publishMilestoneCreated(ConstructionMilestone milestone) {
        log.debug("Publishing milestone created event for {}", milestone.getName());

        MilestoneResponse response = milestoneMapper.toResponse(milestone);
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MILESTONE_CREATED");
        event.put("milestone", response);
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_MILESTONE_EVENTS, milestone.getId().toString(), event);
            log.info("Published milestone created event for {}", milestone.getName());
        } catch (Exception e) {
            log.error("Failed to publish milestone created event: {}", e.getMessage());
        }
    }

    /**
     * Publish milestone updated event.
     */
    @Async
    public void publishMilestoneUpdated(ConstructionMilestone milestone) {
        log.debug("Publishing milestone updated event for {}", milestone.getName());

        MilestoneResponse response = milestoneMapper.toResponse(milestone);
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MILESTONE_UPDATED");
        event.put("milestone", response);
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_MILESTONE_EVENTS, milestone.getId().toString(), event);
            log.info("Published milestone updated event for {}", milestone.getName());
        } catch (Exception e) {
            log.error("Failed to publish milestone updated event: {}", e.getMessage());
        }
    }

    /**
     * Publish milestone completed event.
     */
    @Async
    public void publishMilestoneCompleted(ConstructionMilestone milestone) {
        log.debug("Publishing milestone completed event for {}", milestone.getName());

        MilestoneResponse response = milestoneMapper.toResponse(milestone);
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MILESTONE_COMPLETED");
        event.put("milestone", response);
        event.put("delayDays", milestone.calculateDelayDays());
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_MILESTONE_EVENTS, milestone.getId().toString(), event);
            log.info("Published milestone completed event for {}", milestone.getName());
        } catch (Exception e) {
            log.error("Failed to publish milestone completed event: {}", e.getMessage());
        }
    }
}
