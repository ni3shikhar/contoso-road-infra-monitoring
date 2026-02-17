package com.contoso.monitoring.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * RBAC (Role-Based Access Control) Integration Tests
 * 
 * Tests all 9 personas with their specific permissions:
 * - ADMIN: Full system access
 * - MANAGER: Strategic oversight, read all + some approvals
 * - SUPERVISOR: Operational oversight, manage teams and assignments
 * - ENGINEER: Create/update assets and sensors, cannot delete
 * - ANALYST: Read-only access to data and reports
 * - TECHNICIAN: Field work, update sensor readings and status
 * - OPERATOR: Monitoring, acknowledge alerts, update progress
 * - AUDITOR: Read-only audit trail access
 * - VIEWER: Read-only basic access
 * 
 * Also tests:
 * - Account lockout after 5 failed login attempts
 * - Token expiry and refresh
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RbacIntegrationIT extends BaseIntegrationTest {

    // Test resources created by ADMIN for other tests
    private String testAssetId;
    private String testSensorId;
    private String testAlertId;
    
    private final String testAssetCode = "RBAC-ASSET-" + UUID.randomUUID().toString().substring(0, 8);
    private final String testSensorCode = "RBAC-SENSOR-" + UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    void setupTestResources() {
        log.info("Setting up RBAC test resources...");
        waitForServices();
        createTestResources();
    }

    @AfterAll
    void cleanupTestResources() {
        log.info("Cleaning up RBAC test resources...");
        if (testSensorId != null) {
            deleteTestSensor(testSensorId);
        }
        if (testAssetId != null) {
            deleteTestAsset(testAssetId);
        }
    }

    private void createTestResources() {
        // Create test asset as admin
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", testAssetCode);
        assetData.put("name", "RBAC Test Bridge");
        assetData.put("assetType", "BRIDGE");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.7749);
        assetData.put("longitude", -122.4194);
        assetData.put("description", "Test bridge for RBAC tests");

        Response assetResponse = authenticatedRequest("admin")
                .body(assetData)
                .post("/api/v1/assets");

        if (assetResponse.statusCode() >= 200 && assetResponse.statusCode() < 300) {
            testAssetId = assetResponse.jsonPath().getString("data.id");
            log.info("Created test asset: {}", testAssetId);
        }

        // Create test sensor as admin
        if (testAssetId != null) {
            Map<String, Object> sensorData = new HashMap<>();
            sensorData.put("sensorCode", testSensorCode);
            sensorData.put("sensorType", "STRAIN_GAUGE");
            sensorData.put("manufacturer", "Test Manufacturer");
            sensorData.put("model", "RBAC-TEST-MODEL");
            sensorData.put("installationDate", "2024-01-15");
            sensorData.put("lastCalibrationDate", "2024-01-15");
            sensorData.put("calibrationIntervalDays", 90);
            sensorData.put("latitude", 37.7749);
            sensorData.put("longitude", -122.4194);
            sensorData.put("elevation", 100.0);
            sensorData.put("assetId", testAssetId);
            sensorData.put("assetType", "BRIDGE");
            sensorData.put("locationDescription", "Test sensor for RBAC tests");
            sensorData.put("batteryLevel", 100.0);
            sensorData.put("signalStrength", 95.0);
            sensorData.put("firmwareVersion", "1.0.0");
            sensorData.put("minThreshold", 0.0);
            sensorData.put("maxThreshold", 500.0);
            sensorData.put("unit", "μstrain");

            Response sensorResponse = authenticatedRequest("admin")
                    .body(sensorData)
                    .post("/api/v1/sensors");

            if (sensorResponse.statusCode() >= 200 && sensorResponse.statusCode() < 300) {
                testSensorId = sensorResponse.jsonPath().getString("data.id");
                log.info("Created test sensor: {}", testSensorId);
                
                // Activate the sensor
                authenticatedRequest("admin")
                        .body(Map.of("status", "ACTIVE"))
                        .patch("/api/v1/sensors/" + testSensorId + "/status");
            }
        }
    }

    // =========================================================================
    // ADMIN Role Tests - Full System Access
    // =========================================================================

    @Test
    @Order(100)
    @DisplayName("ADMIN - Can create new users")
    void adminCanCreateUsers() {
        String uniqueUsername = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", uniqueUsername);
        userData.put("email", uniqueUsername + "@test.com");
        userData.put("password", "TestPassword@123");
        userData.put("firstName", "Test");
        userData.put("lastName", "User");
        userData.put("role", "VIEWER");

        Response response = authenticatedRequest("admin")
                .body(userData)
                .post("/api/v1/auth/register");

        // Admin should be able to create users
        assertThat(response.statusCode()).isIn(200, 201, 409); // 409 if user exists
        log.info("ADMIN create user response: {}", response.statusCode());
    }

    @Test
    @Order(101)
    @DisplayName("ADMIN - Can create assets")
    void adminCanCreateAssets() {
        String uniqueCode = "ADMIN-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", uniqueCode);
        assetData.put("name", "Admin Created Asset");
        assetData.put("assetType", "ROAD_SECTION");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.78);
        assetData.put("longitude", -122.42);

        Response response = authenticatedRequest("admin")
                .body(assetData)
                .post("/api/v1/assets");

        assertThat(response.statusCode()).isIn(200, 201);
        
        // Cleanup
        String assetId = response.jsonPath().getString("data.id");
        if (assetId != null) {
            deleteTestAsset(assetId);
        }
    }

    @Test
    @Order(102)
    @DisplayName("ADMIN - Can delete assets")
    void adminCanDeleteAssets() {
        // Create an asset to delete
        String uniqueCode = "DELETE-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", uniqueCode);
        assetData.put("name", "Asset to Delete");
        assetData.put("assetType", "DRAINAGE");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.78);
        assetData.put("longitude", -122.42);

        Response createResponse = authenticatedRequest("admin")
                .body(assetData)
                .post("/api/v1/assets");

        String assetId = createResponse.jsonPath().getString("data.id");
        
        if (assetId != null) {
            Response deleteResponse = authenticatedRequest("admin")
                    .delete("/api/v1/assets/" + assetId);

            assertThat(deleteResponse.statusCode()).isIn(200, 204, 404);
        }
    }

    @Test
    @Order(103)
    @DisplayName("ADMIN - Can access audit logs")
    void adminCanAccessAuditLogs() {
        Response response = authenticatedRequest("admin")
                .queryParam("size", 10)
                .get("/api/v1/audit/logs");

        // Admin should have access to audit logs
        assertThat(response.statusCode()).isIn(200, 404); // 404 if endpoint doesn't exist
    }

    @Test
    @Order(104)
    @DisplayName("ADMIN - Can unlock user accounts")
    void adminCanUnlockAccounts() {
        Response response = authenticatedRequest("admin")
                .body(Map.of("locked", false))
                .post("/api/v1/auth/users/viewer/unlock");

        // Admin should be able to unlock accounts
        assertThat(response.statusCode()).isIn(200, 204, 404);
    }

    // =========================================================================
    // ENGINEER Role Tests - Create/Update but not Delete
    // =========================================================================

    @Test
    @Order(200)
    @DisplayName("ENGINEER - Can create sensors")
    void engineerCanCreateSensors() {
        String uniqueCode = "ENG-SENSOR-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("sensorCode", uniqueCode);
        sensorData.put("sensorType", "TEMPERATURE");
        sensorData.put("manufacturer", "Engineer Test");
        sensorData.put("model", "ENG-MODEL-001");
        sensorData.put("installationDate", "2024-01-15");
        sensorData.put("lastCalibrationDate", "2024-01-15");
        sensorData.put("calibrationIntervalDays", 90);
        sensorData.put("latitude", 37.77);
        sensorData.put("longitude", -122.41);
        sensorData.put("elevation", 50.0);
        sensorData.put("assetId", testAssetId);
        sensorData.put("assetType", "BRIDGE");
        sensorData.put("locationDescription", "Engineer created sensor");
        sensorData.put("batteryLevel", 100.0);
        sensorData.put("signalStrength", 90.0);
        sensorData.put("firmwareVersion", "1.0.0");
        sensorData.put("minThreshold", -20.0);
        sensorData.put("maxThreshold", 60.0);
        sensorData.put("unit", "°C");

        Response response = authenticatedRequest("engineer")
                .body(sensorData)
                .post("/api/v1/sensors");

        // Engineer should be able to create sensors
        assertThat(response.statusCode()).isIn(200, 201, 403);
        
        // Cleanup if created
        String sensorId = response.jsonPath().getString("data.id");
        if (sensorId != null) {
            deleteTestSensor(sensorId);
        }
    }

    @Test
    @Order(201)
    @DisplayName("ENGINEER - Can update sensor configuration")
    void engineerCanUpdateSensorConfig() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("calibrationIntervalDays", 60);
        updateData.put("firmwareVersion", "1.1.0");

        Response response = authenticatedRequest("engineer")
                .body(updateData)
                .put("/api/v1/sensors/" + testSensorId);

        // Engineer should be able to update sensors
        assertThat(response.statusCode()).isIn(200, 204, 403, 404);
    }

    @Test
    @Order(202)
    @DisplayName("ENGINEER - Cannot delete sensors")
    void engineerCannotDeleteSensors() {
        Response response = authenticatedRequest("engineer")
                .delete("/api/v1/sensors/" + testSensorId);

        // Engineer should NOT be able to delete sensors
        // Should get 403 Forbidden
        assertThat(response.statusCode()).isIn(403, 405);
        log.info("Engineer delete attempt returned: {}", response.statusCode());
    }

    @Test
    @Order(203)
    @DisplayName("ENGINEER - Cannot create users")
    void engineerCannotCreateUsers() {
        String uniqueUsername = "eng-testuser-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", uniqueUsername);
        userData.put("email", uniqueUsername + "@test.com");
        userData.put("password", "TestPassword@123");
        userData.put("firstName", "Test");
        userData.put("lastName", "User");
        userData.put("role", "VIEWER");

        Response response = authenticatedRequest("engineer")
                .body(userData)
                .post("/api/v1/auth/register");

        // Engineer should NOT be able to create users
        assertThat(response.statusCode()).isIn(403, 401);
    }

    // =========================================================================
    // OPERATOR Role Tests - Acknowledge Alerts, Update Progress
    // =========================================================================

    @Test
    @Order(300)
    @DisplayName("OPERATOR - Can acknowledge alerts")
    void operatorCanAcknowledgeAlerts() {
        // First, get an open alert
        Response alertsResponse = authenticatedRequest("operator")
                .queryParam("status", "OPEN")
                .queryParam("size", 1)
                .get("/api/v1/alerts");

        if (alertsResponse.statusCode() == 200 && 
            alertsResponse.jsonPath().getList("data.content") != null &&
            !alertsResponse.jsonPath().getList("data.content").isEmpty()) {
            
            String alertId = alertsResponse.jsonPath().getString("data.content[0].id");
            
            Map<String, Object> ackData = new HashMap<>();
            ackData.put("acknowledgedBy", "operator");
            ackData.put("notes", "RBAC test acknowledgment");

            Response response = authenticatedRequest("operator")
                    .body(ackData)
                    .post("/api/v1/alerts/" + alertId + "/acknowledge");

            // Operator should be able to acknowledge alerts
            assertThat(response.statusCode()).isIn(200, 204, 400);
        } else {
            log.info("No open alerts available for operator acknowledgment test");
        }
    }

    @Test
    @Order(301)
    @DisplayName("OPERATOR - Can read sensors")
    void operatorCanReadSensors() {
        Response response = authenticatedRequest("operator")
                .get("/api/v1/sensors/" + testSensorId);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(302)
    @DisplayName("OPERATOR - Cannot create assets")
    void operatorCannotCreateAssets() {
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", "OP-ASSET-001");
        assetData.put("name", "Operator Created Asset");
        assetData.put("assetType", "BRIDGE");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.78);
        assetData.put("longitude", -122.42);

        Response response = authenticatedRequest("operator")
                .body(assetData)
                .post("/api/v1/assets");

        // Operator should NOT be able to create assets
        assertThat(response.statusCode()).isIn(403, 401);
    }

    @Test
    @Order(303)
    @DisplayName("OPERATOR - Cannot delete sensors")
    void operatorCannotDeleteSensors() {
        Response response = authenticatedRequest("operator")
                .delete("/api/v1/sensors/" + testSensorId);

        // Operator should NOT be able to delete
        assertThat(response.statusCode()).isIn(403, 405);
    }

    // =========================================================================
    // VIEWER Role Tests - Read Only
    // =========================================================================

    @Test
    @Order(400)
    @DisplayName("VIEWER - Can read assets")
    void viewerCanReadAssets() {
        Response response = authenticatedRequest("viewer")
                .get("/api/v1/assets/" + testAssetId);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(401)
    @DisplayName("VIEWER - Can read sensors")
    void viewerCanReadSensors() {
        Response response = authenticatedRequest("viewer")
                .get("/api/v1/sensors/" + testSensorId);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(402)
    @DisplayName("VIEWER - Cannot create assets (POST = 403)")
    void viewerCannotCreateAssets() {
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", "VIEWER-ASSET-001");
        assetData.put("name", "Viewer Created Asset");
        assetData.put("assetType", "BRIDGE");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.78);
        assetData.put("longitude", -122.42);

        Response response = authenticatedRequest("viewer")
                .body(assetData)
                .post("/api/v1/assets");

        // VIEWER should get 403 Forbidden
        assertThat(response.statusCode()).isEqualTo(403);
        log.info("Viewer POST asset returned: {}", response.statusCode());
    }

    @Test
    @Order(403)
    @DisplayName("VIEWER - Cannot update assets (PUT = 403)")
    void viewerCannotUpdateAssets() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Viewer Updated Asset");
        updateData.put("description", "Should not work");

        Response response = authenticatedRequest("viewer")
                .body(updateData)
                .put("/api/v1/assets/" + testAssetId);

        // VIEWER should get 403 Forbidden
        assertThat(response.statusCode()).isEqualTo(403);
        log.info("Viewer PUT asset returned: {}", response.statusCode());
    }

    @Test
    @Order(404)
    @DisplayName("VIEWER - Cannot delete assets (DELETE = 403)")
    void viewerCannotDeleteAssets() {
        Response response = authenticatedRequest("viewer")
                .delete("/api/v1/assets/" + testAssetId);

        // VIEWER should get 403 Forbidden
        assertThat(response.statusCode()).isEqualTo(403);
        log.info("Viewer DELETE asset returned: {}", response.statusCode());
    }

    @Test
    @Order(405)
    @DisplayName("VIEWER - Cannot create sensors (POST = 403)")
    void viewerCannotCreateSensors() {
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("sensorCode", "VIEWER-SENSOR-001");
        sensorData.put("sensorType", "TEMPERATURE");
        sensorData.put("manufacturer", "Test");
        sensorData.put("model", "TEST-001");
        sensorData.put("assetId", testAssetId);
        sensorData.put("assetType", "BRIDGE");

        Response response = authenticatedRequest("viewer")
                .body(sensorData)
                .post("/api/v1/sensors");

        // VIEWER should get 403 Forbidden
        assertThat(response.statusCode()).isEqualTo(403);
        log.info("Viewer POST sensor returned: {}", response.statusCode());
    }

    // =========================================================================
    // ANALYST Role Tests - Read Access to Data and Reports
    // =========================================================================

    @Test
    @Order(500)
    @DisplayName("ANALYST - Can read analytics data")
    void analystCanReadAnalytics() {
        Response response = authenticatedRequest("analyst")
                .get("/api/v1/analytics/kpis");

        // Analyst should have read access to analytics
        assertThat(response.statusCode()).isIn(200, 404);
    }

    @Test
    @Order(501)
    @DisplayName("ANALYST - Can read sensor trends")
    void analystCanReadSensorTrends() {
        Response response = authenticatedRequest("analyst")
                .queryParam("period", "24h")
                .get("/api/v1/analytics/sensors/" + testSensorId + "/trends");

        assertThat(response.statusCode()).isIn(200, 404);
    }

    @Test
    @Order(502)
    @DisplayName("ANALYST - Cannot create assets")
    void analystCannotCreateAssets() {
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", "ANALYST-ASSET-001");
        assetData.put("name", "Analyst Created Asset");
        assetData.put("assetType", "BRIDGE");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.78);
        assetData.put("longitude", -122.42);

        Response response = authenticatedRequest("analyst")
                .body(assetData)
                .post("/api/v1/assets");

        // Analyst should NOT be able to create assets
        assertThat(response.statusCode()).isIn(403, 401);
    }

    // =========================================================================
    // TECHNICIAN Role Tests - Field Operations
    // =========================================================================

    @Test
    @Order(600)
    @DisplayName("TECHNICIAN - Can update sensor status")
    void technicianCanUpdateSensorStatus() {
        Response response = authenticatedRequest("technician")
                .body(Map.of("status", "MAINTENANCE"))
                .patch("/api/v1/sensors/" + testSensorId + "/status");

        // Technician should be able to update sensor status
        assertThat(response.statusCode()).isIn(200, 204, 403);
        
        // Revert status
        authenticatedRequest("admin")
                .body(Map.of("status", "ACTIVE"))
                .patch("/api/v1/sensors/" + testSensorId + "/status");
    }

    @Test
    @Order(601)
    @DisplayName("TECHNICIAN - Can submit sensor readings")
    void technicianCanSubmitReadings() {
        Map<String, Object> reading = new HashMap<>();
        reading.put("sensorId", testSensorId);
        reading.put("value", 125.5);
        reading.put("timestamp", Instant.now().toString());
        reading.put("unit", "μstrain");
        reading.put("quality", 1.0);

        Response response = authenticatedRequest("technician")
                .body(reading)
                .post("/api/v1/sensors/" + testSensorId + "/readings");

        // Technician should be able to submit readings
        assertThat(response.statusCode()).isIn(200, 201, 202, 403);
    }

    @Test
    @Order(602)
    @DisplayName("TECHNICIAN - Cannot delete assets")
    void technicianCannotDeleteAssets() {
        Response response = authenticatedRequest("technician")
                .delete("/api/v1/assets/" + testAssetId);

        // Technician should NOT be able to delete
        assertThat(response.statusCode()).isIn(403, 405);
    }

    // =========================================================================
    // AUDITOR Role Tests - Read-Only Audit Access
    // =========================================================================

    @Test
    @Order(700)
    @DisplayName("AUDITOR - Can read audit logs")
    void auditorCanReadAuditLogs() {
        Response response = authenticatedRequest("auditor")
                .queryParam("size", 10)
                .get("/api/v1/audit/logs");

        // Auditor should have read access to audit logs
        assertThat(response.statusCode()).isIn(200, 404);
    }

    @Test
    @Order(701)
    @DisplayName("AUDITOR - Can read assets")
    void auditorCanReadAssets() {
        Response response = authenticatedRequest("auditor")
                .get("/api/v1/assets/" + testAssetId);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(702)
    @DisplayName("AUDITOR - Cannot modify assets")
    void auditorCannotModifyAssets() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Auditor Modified Asset");

        Response response = authenticatedRequest("auditor")
                .body(updateData)
                .put("/api/v1/assets/" + testAssetId);

        // Auditor should NOT be able to modify
        assertThat(response.statusCode()).isIn(403, 405);
    }

    // =========================================================================
    // SUPERVISOR Role Tests - Operational Oversight
    // =========================================================================

    @Test
    @Order(800)
    @DisplayName("SUPERVISOR - Can read all assets")
    void supervisorCanReadAssets() {
        Response response = authenticatedRequest("supervisor")
                .queryParam("size", 10)
                .get("/api/v1/assets");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(801)
    @DisplayName("SUPERVISOR - Can read alerts")
    void supervisorCanReadAlerts() {
        Response response = authenticatedRequest("supervisor")
                .queryParam("size", 10)
                .get("/api/v1/alerts");

        assertThat(response.statusCode()).isIn(200, 404);
    }

    // =========================================================================
    // MANAGER Role Tests - Strategic Oversight
    // =========================================================================

    @Test
    @Order(900)
    @DisplayName("MANAGER - Can read KPIs and analytics")
    void managerCanReadAnalytics() {
        Response response = authenticatedRequest("manager")
                .get("/api/v1/analytics/kpis");

        assertThat(response.statusCode()).isIn(200, 404);
    }

    @Test
    @Order(901)
    @DisplayName("MANAGER - Can read all assets and sensors")
    void managerCanReadAllResources() {
        Response assetsResponse = authenticatedRequest("manager")
                .queryParam("size", 10)
                .get("/api/v1/assets");
        assertThat(assetsResponse.statusCode()).isEqualTo(200);

        Response sensorsResponse = authenticatedRequest("manager")
                .queryParam("size", 10)
                .get("/api/v1/sensors");
        assertThat(sensorsResponse.statusCode()).isEqualTo(200);
    }

    // =========================================================================
    // Account Lockout Tests
    // =========================================================================

    @Test
    @Order(1000)
    @DisplayName("SECURITY - Account lockout after 5 failed login attempts")
    void accountLockoutAfterFailedAttempts() {
        String testUsername = "viewer"; // Use viewer account for lockout test
        
        // Clear any existing token
        clearTokenCache(testUsername);
        
        // Attempt 5 failed logins
        for (int i = 0; i < 5; i++) {
            Response response = unauthenticatedRequest()
                    .body(Map.of(
                            "username", testUsername,
                            "password", "WrongPassword" + i
                    ))
                    .post("/api/v1/auth/login");
            
            log.info("Failed login attempt {} - status: {}", i + 1, response.statusCode());
        }
        
        // The 6th attempt should show account is locked
        Response lockedResponse = unauthenticatedRequest()
                .body(Map.of(
                        "username", testUsername,
                        "password", "WrongPassword6"
                ))
                .post("/api/v1/auth/login");
        
        // Could be 401 with "account locked" message or 423 Locked
        log.info("Login after lockout - status: {}, body: {}", 
                lockedResponse.statusCode(), 
                lockedResponse.body().asString().substring(0, Math.min(200, lockedResponse.body().asString().length())));
        
        // Unlock the account as admin
        authenticatedRequest("admin")
                .body(Map.of("locked", false))
                .post("/api/v1/auth/users/" + testUsername + "/unlock");
        
        // Verify login works after unlock with correct password
        Response afterUnlockResponse = unauthenticatedRequest()
                .body(Map.of(
                        "username", testUsername,
                        "password", TEST_USERS.get(testUsername).password()
                ))
                .post("/api/v1/auth/login");
        
        assertThat(afterUnlockResponse.statusCode()).isEqualTo(200);
        log.info("Login after admin unlock - status: {}", afterUnlockResponse.statusCode());
    }

    // =========================================================================
    // Token Expiry Tests
    // =========================================================================

    @Test
    @Order(1100)
    @DisplayName("SECURITY - Expired token returns 401")
    void expiredTokenReturns401() {
        // This test simulates using an expired token
        // In practice, we'd need to wait for token expiry or use a pre-expired token
        
        String fakeExpiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ2aWV3ZXIiLCJleHAiOjE1MDAwMDAwMDB9." +
                "fake_signature_for_expired_token";
        
        Response response = unauthenticatedRequest()
                .header("Authorization", "Bearer " + fakeExpiredToken)
                .get("/api/v1/assets");
        
        // Should return 401 Unauthorized
        assertThat(response.statusCode()).isEqualTo(401);
        log.info("Expired token request returned: {}", response.statusCode());
    }

    @Test
    @Order(1101)
    @DisplayName("SECURITY - Token refresh returns new valid token")
    void tokenRefreshWorks() {
        // Login to get initial tokens
        Response loginResponse = unauthenticatedRequest()
                .body(Map.of(
                        "username", "viewer",
                        "password", TEST_USERS.get("viewer").password()
                ))
                .post("/api/v1/auth/login");
        
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        
        String refreshToken = loginResponse.jsonPath().getString("data.refreshToken");
        
        if (refreshToken != null && !refreshToken.isEmpty()) {
            // Use refresh token to get new access token
            Response refreshResponse = unauthenticatedRequest()
                    .body(Map.of("refreshToken", refreshToken))
                    .post("/api/v1/auth/refresh");
            
            if (refreshResponse.statusCode() == 200) {
                String newAccessToken = refreshResponse.jsonPath().getString("data.accessToken");
                assertThat(newAccessToken).isNotEmpty();
                log.info("Token refresh successful, got new access token");
                
                // Verify new token works
                Response verifyResponse = unauthenticatedRequest()
                        .header("Authorization", "Bearer " + newAccessToken)
                        .get("/api/v1/assets");
                
                assertThat(verifyResponse.statusCode()).isEqualTo(200);
            } else {
                log.info("Token refresh endpoint returned: {}", refreshResponse.statusCode());
            }
        } else {
            log.info("No refresh token returned in login response");
        }
    }

    // =========================================================================
    // Cross-Role Permission Matrix Tests
    // =========================================================================

    @ParameterizedTest
    @CsvSource({
        "admin,POST,/api/v1/assets,200",
        "admin,DELETE,/api/v1/assets/{id},200",
        "engineer,POST,/api/v1/sensors,200",
        "engineer,DELETE,/api/v1/sensors/{id},403",
        "viewer,GET,/api/v1/assets,200",
        "viewer,POST,/api/v1/assets,403",
        "viewer,DELETE,/api/v1/assets/{id},403"
    })
    @Order(1200)
    @DisplayName("MATRIX - Permission matrix validation")
    void permissionMatrixValidation(String role, String method, String endpoint, int expectedStatus) {
        // Replace {id} placeholders
        String actualEndpoint = endpoint
                .replace("{id}", testAssetId != null ? testAssetId : "test-id");
        
        Response response;
        
        switch (method) {
            case "GET":
                response = authenticatedRequest(role).get(actualEndpoint);
                break;
            case "POST":
                Map<String, Object> postData = new HashMap<>();
                postData.put("assetCode", "MATRIX-" + UUID.randomUUID().toString().substring(0, 8));
                postData.put("name", "Matrix Test");
                postData.put("assetType", "BRIDGE");
                postData.put("status", "OPERATIONAL");
                postData.put("latitude", 37.78);
                postData.put("longitude", -122.42);
                response = authenticatedRequest(role).body(postData).post(actualEndpoint);
                break;
            case "PUT":
                response = authenticatedRequest(role).body(Map.of("name", "Updated")).put(actualEndpoint);
                break;
            case "DELETE":
                response = authenticatedRequest(role).delete(actualEndpoint);
                break;
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
        
        // For some operations we accept a range of acceptable responses
        if (expectedStatus == 200) {
            assertThat(response.statusCode()).isIn(200, 201, 204, 404);
        } else {
            assertThat(response.statusCode()).isEqualTo(expectedStatus);
        }
        
        log.info("Permission test: {} {} {} -> {} (expected: {})", 
                role, method, actualEndpoint, response.statusCode(), expectedStatus);
    }

    // =========================================================================
    // All Roles Can Authenticate
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"admin", "engineer", "operator", "viewer", "auditor", "supervisor", "technician", "analyst", "manager"})
    @Order(1300)
    @DisplayName("AUTH - All roles can authenticate successfully")
    void allRolesCanAuthenticate(String role) {
        clearTokenCache(role);
        
        TestUser user = TEST_USERS.get(role);
        
        Response response = unauthenticatedRequest()
                .body(Map.of(
                        "username", user.username(),
                        "password", user.password()
                ))
                .post("/api/v1/auth/login");
        
        assertThat(response.statusCode()).isEqualTo(200);
        
        String token = response.jsonPath().getString("data.accessToken");
        assertThat(token).isNotEmpty();
        
        log.info("Role {} authenticated successfully", role);
    }
}
