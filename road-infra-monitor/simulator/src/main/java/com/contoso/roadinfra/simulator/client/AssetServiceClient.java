package com.contoso.roadinfra.simulator.client;

import com.contoso.roadinfra.simulator.model.AssetResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for interacting with the asset-service API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetServiceClient {

    private final WebClient webClient;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    /**
     * Get all assets from the asset-service.
     */
    public List<AssetResponse> getAllAssets() {
        log.info("Fetching all assets from asset-service");
        List<AssetResponse> allAssets = new ArrayList<>();
        int page = 0;
        int size = 100;
        boolean hasMore = true;

        while (hasMore) {
            final int currentPage = page;
            final int currentSize = size;
            try {
                String responseBody = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/assets")
                                .queryParam("page", currentPage)
                                .queryParam("size", currentSize)
                                .build())
                        .header("Authorization", "Bearer " + authClient.getAccessToken())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> {
                            log.error("Error fetching assets: {}", response.statusCode());
                            return Mono.error(new RuntimeException("Failed to fetch assets"));
                        })
                        .bodyToMono(String.class)
                        .block();

                if (responseBody != null) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode data = root.get("data");
                    
                    if (data != null && data.has("content")) {
                        List<AssetResponse> assets = objectMapper.convertValue(
                                data.get("content"),
                                new TypeReference<List<AssetResponse>>() {}
                        );
                        allAssets.addAll(assets);
                        
                        boolean isLast = data.has("last") && data.get("last").asBoolean();
                        hasMore = !isLast && !assets.isEmpty();
                        page++;
                    } else {
                        hasMore = false;
                    }
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                log.error("Error fetching assets page {}: {}", page, e.getMessage());
                hasMore = false;
            }
        }

        log.info("Fetched {} assets", allAssets.size());
        return allAssets;
    }
}
