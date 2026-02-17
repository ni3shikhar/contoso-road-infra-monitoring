package com.contoso.roadinfra.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/sensors")
    public Mono<ResponseEntity<Map<String, Object>>> sensorFallback() {
        return createFallbackResponse("Sensor Service");
    }

    @RequestMapping("/assets")
    public Mono<ResponseEntity<Map<String, Object>>> assetFallback() {
        return createFallbackResponse("Asset Service");
    }

    @RequestMapping("/monitoring")
    public Mono<ResponseEntity<Map<String, Object>>> monitoringFallback() {
        return createFallbackResponse("Monitoring Service");
    }

    @RequestMapping("/alerts")
    public Mono<ResponseEntity<Map<String, Object>>> alertFallback() {
        return createFallbackResponse("Alert Service");
    }

    @RequestMapping("/analytics")
    public Mono<ResponseEntity<Map<String, Object>>> analyticsFallback() {
        return createFallbackResponse("Analytics Service");
    }

    @RequestMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        return createFallbackResponse("Auth Service");
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(String serviceName) {
        Map<String, Object> response = Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", serviceName + " is currently unavailable. Please try again later.",
                "timestamp", LocalDateTime.now().toString()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
