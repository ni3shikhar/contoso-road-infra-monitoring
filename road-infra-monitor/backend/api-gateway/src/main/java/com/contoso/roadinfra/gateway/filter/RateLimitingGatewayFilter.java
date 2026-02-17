package com.contoso.roadinfra.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Role-based rate limiting filter using Redis.
 * Rate limits requests based on user role.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingGatewayFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    // Rate limits per minute by role
    private static final Map<String, Integer> ROLE_RATE_LIMITS = Map.of(
            "ADMIN", 500,
            "ENGINEER", 300,
            "OPERATOR", 200,
            "VIEWER", 100
    );

    private static final int DEFAULT_RATE_LIMIT = 60;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Get user info from headers (set by JwtValidationGatewayFilter)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");

        // Skip rate limiting for unauthenticated requests (they're either public or will be rejected)
        if (userId == null || userId.isEmpty()) {
            return chain.filter(exchange);
        }

        int rateLimit = getRateLimitForRole(role);
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;

        return checkRateLimit(rateLimitKey, rateLimit)
                .flatMap(allowed -> {
                    if (!allowed) {
                        log.warn("Rate limit exceeded for user: {} (role: {}, limit: {})", 
                                userId, role, rateLimit);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimit));
                        exchange.getResponse().getHeaders().add("Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }

                    // Add rate limit headers
                    return getCurrentCount(rateLimitKey)
                            .doOnNext(count -> {
                                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                                        String.valueOf(rateLimit));
                                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
                                        String.valueOf(Math.max(0, rateLimit - count.intValue())));
                            })
                            .then(chain.filter(exchange));
                });
    }

    private int getRateLimitForRole(String role) {
        if (role == null || role.isEmpty()) {
            return DEFAULT_RATE_LIMIT;
        }
        return ROLE_RATE_LIMITS.getOrDefault(role, DEFAULT_RATE_LIMIT);
    }

    private Mono<Boolean> checkRateLimit(String key, int limit) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        // First request, set expiry
                        return redisTemplate.expire(key, RATE_LIMIT_WINDOW)
                                .thenReturn(true);
                    }
                    return Mono.just(count <= limit);
                })
                .onErrorResume(e -> {
                    log.error("Redis error during rate limiting, allowing request: {}", e.getMessage());
                    return Mono.just(true); // Fail open
                });
    }

    private Mono<Long> getCurrentCount(String key) {
        return redisTemplate.opsForValue().get(key)
                .map(Long::parseLong)
                .defaultIfEmpty(0L)
                .onErrorReturn(0L);
    }

    @Override
    public int getOrder() {
        // Run after JWT validation
        return -90;
    }
}
