package com.contoso.roadinfra.simulator.client;

import com.contoso.roadinfra.simulator.config.ApiConfig;
import com.contoso.roadinfra.simulator.model.ApiResponse;
import com.contoso.roadinfra.simulator.model.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client for authentication with the auth-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final WebClient webClient;
    private final ApiConfig apiConfig;

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<String> refreshToken = new AtomicReference<>();
    private final AtomicReference<Instant> tokenExpiry = new AtomicReference<>();

    /**
     * Get a valid access token, refreshing if necessary.
     */
    public String getAccessToken() {
        if (accessToken.get() == null || isTokenExpired()) {
            synchronized (this) {
                if (accessToken.get() == null || isTokenExpired()) {
                    if (refreshToken.get() != null) {
                        try {
                            refreshAccessToken();
                        } catch (Exception e) {
                            log.warn("Failed to refresh token, performing full login: {}", e.getMessage());
                            login();
                        }
                    } else {
                        login();
                    }
                }
            }
        }
        return accessToken.get();
    }

    /**
     * Perform login and store tokens.
     */
    public void login() {
        log.info("Logging in as {}", apiConfig.getAuth().getUsername());

        Map<String, String> loginRequest = Map.of(
                "username", apiConfig.getAuth().getUsername(),
                "password", apiConfig.getAuth().getPassword()
        );

        ApiResponse<AuthResponse> response = webClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {})
                .block();

        if (response != null && response.getData() != null) {
            AuthResponse auth = response.getData();
            accessToken.set(auth.getAccessToken());
            refreshToken.set(auth.getRefreshToken());
            // Set expiry 1 minute before actual expiry for safety margin
            tokenExpiry.set(Instant.now().plusSeconds(auth.getExpiresIn() - 60));
            log.info("Login successful, token valid until {}", tokenExpiry.get());
        } else {
            throw new RuntimeException("Login failed - no response data");
        }
    }

    /**
     * Refresh the access token using the refresh token.
     */
    public void refreshAccessToken() {
        log.debug("Refreshing access token");

        Map<String, String> refreshRequest = Map.of(
                "refreshToken", refreshToken.get()
        );

        ApiResponse<AuthResponse> response = webClient.post()
                .uri("/api/v1/auth/refresh")
                .bodyValue(refreshRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {})
                .block();

        if (response != null && response.getData() != null) {
            AuthResponse auth = response.getData();
            accessToken.set(auth.getAccessToken());
            if (auth.getRefreshToken() != null) {
                refreshToken.set(auth.getRefreshToken());
            }
            tokenExpiry.set(Instant.now().plusSeconds(auth.getExpiresIn() - 60));
            log.debug("Token refreshed, valid until {}", tokenExpiry.get());
        } else {
            throw new RuntimeException("Token refresh failed");
        }
    }

    /**
     * Check if the current token is expired or about to expire.
     */
    private boolean isTokenExpired() {
        Instant expiry = tokenExpiry.get();
        return expiry == null || Instant.now().isAfter(expiry);
    }

    /**
     * Clear stored tokens (for logout or forced re-login).
     */
    public void clearTokens() {
        accessToken.set(null);
        refreshToken.set(null);
        tokenExpiry.set(null);
    }
}
