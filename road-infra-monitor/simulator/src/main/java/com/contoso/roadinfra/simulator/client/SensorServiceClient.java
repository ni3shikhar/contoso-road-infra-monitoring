package com.contoso.roadinfra.simulator.client;

import com.contoso.roadinfra.simulator.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

/**
 * Client for interacting with the sensor-service API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorServiceClient {

    private final WebClient webClient;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    /**
     * Get all sensors from the sensor-service.
     */
    public List<SensorResponse> getAllSensors() {
        log.info("Fetching all sensors from sensor-service");
        List<SensorResponse> allSensors = new ArrayList<>();
        int page = 0;
        int size = 100;
        boolean hasMore = true;

        while (hasMore) {
            final int currentPage = page;
            final int currentSize = size;
            try {
                String responseBody = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/sensors")
                                .queryParam("page", currentPage)
                                .queryParam("size", currentSize)
                                .build())
                        .header("Authorization", "Bearer " + authClient.getAccessToken())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> {
                            log.error("Error fetching sensors: {}", response.statusCode());
                            return Mono.error(new RuntimeException("Failed to fetch sensors"));
                        })
                        .bodyToMono(String.class)
                        .block();

                if (responseBody != null) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode data = root.get("data");
                    
                    if (data != null && data.has("content")) {
                        List<SensorResponse> sensors = objectMapper.convertValue(
                                data.get("content"),
                                new TypeReference<List<SensorResponse>>() {}
                        );
                        allSensors.addAll(sensors);
                        
                        boolean isLast = data.has("last") && data.get("last").asBoolean();
                        hasMore = !isLast && !sensors.isEmpty();
                        page++;
                    } else {
                        hasMore = false;
                    }
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                log.error("Error fetching sensors page {}: {}", page, e.getMessage());
                hasMore = false;
            }
        }

        log.info("Fetched {} sensors", allSensors.size());
        return allSensors;
    }

    /**
     * Post a sensor reading.
     */
    public boolean postReading(UUID sensorId, SensorReadingRequest reading) {
        try {
            String response = webClient.post()
                    .uri("/api/v1/sensors/{id}/readings", sensorId)
                    .header("Authorization", "Bearer " + authClient.getAccessToken())
                    .bodyValue(reading)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> {
                        log.warn("Failed to post reading for sensor {}: {}", sensorId, resp.statusCode());
                        return Mono.empty();
                    })
                    .bodyToMono(String.class)
                    .block();

            return response != null;
        } catch (Exception e) {
            log.warn("Error posting reading for sensor {}: {}", sensorId, e.getMessage());
            return false;
        }
    }

    /**
     * Batch post sensor readings.
     */
    public int postBatchReadings(Map<UUID, SensorReadingRequest> readings) {
        List<Map<String, Object>> batchRequest = new ArrayList<>();
        for (Map.Entry<UUID, SensorReadingRequest> entry : readings.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("sensorId", entry.getKey());
            item.put("timestamp", entry.getValue().getTimestamp() != null ? 
                    entry.getValue().getTimestamp().toString() : Instant.now().toString());
            item.put("value", entry.getValue().getValue());
            item.put("unit", entry.getValue().getUnit());
            item.put("quality", entry.getValue().getQuality() != null ? 
                    entry.getValue().getQuality() : "GOOD");
            if (entry.getValue().getSecondaryValue() != null) {
                item.put("secondaryValue", entry.getValue().getSecondaryValue());
            }
            if (entry.getValue().getTertiaryValue() != null) {
                item.put("tertiaryValue", entry.getValue().getTertiaryValue());
            }
            batchRequest.add(item);
        }

        try {
            Map<String, Object> request = Map.of("readings", batchRequest);
            
            String response = webClient.post()
                    .uri("/api/v1/sensors/readings/batch")
                    .header("Authorization", "Bearer " + authClient.getAccessToken())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> {
                        log.warn("Failed to post batch readings: {}", resp.statusCode());
                        return Mono.empty();
                    })
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.get("data");
                return data != null && data.isArray() ? data.size() : 0;
            }
        } catch (Exception e) {
            log.warn("Error posting batch readings: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Update sensor status.
     */
    public boolean updateSensorStatus(UUID sensorId, SensorStatus status) {
        try {
            Map<String, String> request = Map.of("status", status.name());
            
            String response = webClient.patch()
                    .uri("/api/v1/sensors/{id}/status", sensorId)
                    .header("Authorization", "Bearer " + authClient.getAccessToken())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> {
                        log.warn("Failed to update sensor status {}: {}", sensorId, resp.statusCode());
                        return Mono.empty();
                    })
                    .bodyToMono(String.class)
                    .block();

            return response != null;
        } catch (Exception e) {
            log.warn("Error updating sensor {} status: {}", sensorId, e.getMessage());
            return false;
        }
    }
}
