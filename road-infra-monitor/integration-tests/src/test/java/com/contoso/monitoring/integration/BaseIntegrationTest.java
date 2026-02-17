package com.contoso.monitoring.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

/**
 * Base class for integration tests providing common authentication and utility methods.
 */
public abstract class BaseIntegrationTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);
    
    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    // Token cache for different users
    private static final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    // Test users with their credentials
    protected static final Map<String, TestUser> TEST_USERS = Map.of(
            "admin", new TestUser("admin", "Admin@123", "ADMIN"),
            "engineer", new TestUser("engineer", "Engineer@123", "ENGINEER"),
            "operator", new TestUser("operator", "Operator@123", "OPERATOR"),
            "viewer", new TestUser("viewer", "Viewer@123", "VIEWER"),
            "auditor", new TestUser("auditor", "Auditor@123", "AUDITOR"),
            "supervisor", new TestUser("supervisor", "Supervisor@123", "SUPERVISOR"),
            "technician", new TestUser("technician", "Technician@123", "TECHNICIAN"),
            "analyst", new TestUser("analyst", "Analyst@123", "ANALYST"),
            "manager", new TestUser("manager", "Manager@123", "MANAGER")
    );
    
    protected static String apiGatewayUrl;
    
    @BeforeAll
    static void setupBaseUrl() {
        apiGatewayUrl = System.getProperty("api.gateway.url", "http://localhost:8080");
        RestAssured.baseURI = apiGatewayUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        log.info("API Gateway URL: {}", apiGatewayUrl);
    }
    
    @BeforeEach
    void waitForServicesBeforeEachTest() {
        // Ensure services are ready before each test
        waitForServices();
    }
    
    /**
     * Wait for all required services to be healthy.
     */
    protected void waitForServices() {
        await()
            .atMost(Duration.ofMinutes(5))
            .pollInterval(Duration.ofSeconds(5))
            .until(this::isApiGatewayHealthy);
    }
    
    private boolean isApiGatewayHealthy() {
        try {
            Response response = given()
                    .get("/actuator/health");
            return response.statusCode() == 200 && 
                   response.jsonPath().getString("status").equals("UP");
        } catch (Exception e) {
            log.warn("API Gateway health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get or create an authentication token for the specified user.
     */
    protected String getToken(String username) {
        TokenInfo tokenInfo = tokenCache.get(username);
        
        if (tokenInfo == null || tokenInfo.isExpired()) {
            String newToken = authenticate(username);
            tokenCache.put(username, new TokenInfo(newToken));
            return newToken;
        }
        
        return tokenInfo.token;
    }
    
    /**
     * Authenticate and get a fresh token for the specified user.
     */
    protected String authenticate(String username) {
        TestUser user = TEST_USERS.get(username);
        if (user == null) {
            throw new IllegalArgumentException("Unknown test user: " + username);
        }
        
        log.info("Authenticating as user: {}", username);
        
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "username", user.username,
                        "password", user.password
                ))
                .post("/api/v1/auth/login");
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Authentication failed for " + username + 
                    ": " + response.statusCode() + " - " + response.body().asString());
        }
        
        String token = response.jsonPath().getString("data.accessToken");
        log.info("Successfully authenticated as {} with role {}", username, user.role);
        
        return token;
    }
    
    /**
     * Clear the token cache for a specific user.
     */
    protected void clearTokenCache(String username) {
        tokenCache.remove(username);
    }
    
    /**
     * Clear all cached tokens.
     */
    protected void clearAllTokenCache() {
        tokenCache.clear();
    }
    
    /**
     * Create an authenticated request specification for the specified user.
     */
    protected RequestSpecification authenticatedRequest(String username) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + getToken(username));
    }
    
    /**
     * Create an unauthenticated request specification.
     */
    protected RequestSpecification unauthenticatedRequest() {
        return given()
                .contentType(ContentType.JSON);
    }
    
    /**
     * Create an asset for testing and return its ID.
     */
    protected String createTestAsset(String username, Map<String, Object> assetData) {
        Response response = authenticatedRequest(username)
                .body(assetData)
                .post("/api/v1/assets");
        
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Failed to create test asset: " + 
                    response.statusCode() + " - " + response.body().asString());
        }
        
        return response.jsonPath().getString("data.id");
    }
    
    /**
     * Create a sensor for testing and return its ID.
     */
    protected String createTestSensor(String username, Map<String, Object> sensorData) {
        Response response = authenticatedRequest(username)
                .body(sensorData)
                .post("/api/v1/sensors");
        
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Failed to create test sensor: " + 
                    response.statusCode() + " - " + response.body().asString());
        }
        
        return response.jsonPath().getString("data.id");
    }
    
    /**
     * Clean up a test asset.
     */
    protected void deleteTestAsset(String assetId) {
        try {
            authenticatedRequest("admin")
                    .delete("/api/v1/assets/" + assetId);
        } catch (Exception e) {
            log.warn("Failed to delete test asset {}: {}", assetId, e.getMessage());
        }
    }
    
    /**
     * Clean up a test sensor.
     */
    protected void deleteTestSensor(String sensorId) {
        try {
            authenticatedRequest("admin")
                    .delete("/api/v1/sensors/" + sensorId);
        } catch (Exception e) {
            log.warn("Failed to delete test sensor {}: {}", sensorId, e.getMessage());
        }
    }
    
    /**
     * Parse JSON response body.
     */
    protected JsonNode parseJson(Response response) {
        try {
            return objectMapper.readTree(response.body().asString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }
    
    /**
     * Generate a unique test identifier.
     */
    protected String uniqueId() {
        return "test-" + System.currentTimeMillis() + "-" + 
               (int) (Math.random() * 10000);
    }
    
    /**
     * Test user record.
     */
    protected record TestUser(String username, String password, String role) {}
    
    /**
     * Token info with expiration tracking.
     */
    private static class TokenInfo {
        final String token;
        final long createdAt;
        static final long TOKEN_LIFETIME_MS = 14 * 60 * 1000; // 14 minutes
        
        TokenInfo(String token) {
            this.token = token;
            this.createdAt = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TOKEN_LIFETIME_MS;
        }
    }
}
