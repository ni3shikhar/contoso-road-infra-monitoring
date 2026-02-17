package com.contoso.roadinfra.analytics.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for asset-service.
 */
@FeignClient(name = "asset-service", fallback = AssetServiceClientFallback.class)
public interface AssetServiceClient {

    @GetMapping("/api/v1/assets")
    @CircuitBreaker(name = "assetService", fallbackMethod = "getAllAssetsFallback")
    ApiResponse<List<Map<String, Object>>> getAllAssets(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size);

    @GetMapping("/api/v1/assets/{id}")
    @CircuitBreaker(name = "assetService", fallbackMethod = "getAssetByIdFallback")
    ApiResponse<Map<String, Object>> getAssetById(@PathVariable UUID id);

    @GetMapping("/api/v1/assets/type/{type}")
    @CircuitBreaker(name = "assetService", fallbackMethod = "getAssetsByTypeFallback")
    ApiResponse<List<Map<String, Object>>> getAssetsByType(@PathVariable String type);

    @GetMapping("/api/v1/assets/statistics")
    @CircuitBreaker(name = "assetService", fallbackMethod = "getAssetStatisticsFallback")
    ApiResponse<Map<String, Object>> getAssetStatistics();

    @GetMapping("/api/v1/assets/count-by-type")
    @CircuitBreaker(name = "assetService", fallbackMethod = "getAssetCountByTypeFallback")
    ApiResponse<Map<String, Long>> getAssetCountByType();

    @GetMapping("/api/v1/assets/count-by-status")
    @CircuitBreaker(name = "assetService", fallbackMethod = "getAssetCountByStatusFallback")
    ApiResponse<Map<String, Long>> getAssetCountByStatus();
}
