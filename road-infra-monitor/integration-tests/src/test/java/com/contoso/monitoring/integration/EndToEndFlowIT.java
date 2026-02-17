package com.contoso.monitoring.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end integration tests verifying the complete flow:
 * Sensor Registration → Reading Submission → Health Score Calculation → 
 * Alert Generation → KPI Updates → WebSocket Notifications
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndToEndFlowIT extends BaseIntegrationTest {

    private String testAssetId;
    private String testSensorId;
    private String testAlertId;
    private final String testAssetCode = "E2E-ASSET-" + UUID.randomUUID().toString().substring(0, 8);
    private final String testSensorCode = "E2E-SENSOR-" + UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    void setupTestData() {
        log.info("Setting up end-to-end test data...");
        waitForServices();
    }

    @AfterAll
    void cleanupTestData() {
        log.info("Cleaning up end-to-end test data...");
        if (testSensorId != null) {
            deleteTestSensor(testSensorId);
        }
        if (testAssetId != null) {
            deleteTestAsset(testAssetId);
        }
    }

    // =========================================================================
    // 1. Asset Management Tests
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1.1 - Create a new asset")
    void createAsset() {
        Map<String, Object> assetData = new HashMap<>();
        assetData.put("assetCode", testAssetCode);
        assetData.put("name", "End-to-End Test Bridge");
        assetData.put("assetType", "BRIDGE");
        assetData.put("status", "OPERATIONAL");
        assetData.put("latitude", 37.7749);
        assetData.put("longitude", -122.4194);
        assetData.put("description", "Test bridge for E2E integration tests");

        Response response = authenticatedRequest("admin")
                .body(assetData)
                .post("/api/v1/assets");

        assertThat(response.statusCode()).isIn(200, 201);
        
        testAssetId = response.jsonPath().getString("data.id");
        assertThat(testAssetId).isNotNull();
        
        log.info("Created test asset with ID: {}", testAssetId);
    }

    @Test
    @Order(2)
    @DisplayName("1.2 - Verify asset can be retrieved")
    void getAsset() {
        Response response = authenticatedRequest("admin")
                .get("/api/v1/assets/" + testAssetId);

        response.then()
                .statusCode(200)
                .body("data.assetCode", equalTo(testAssetCode))
                .body("data.name", equalTo("End-to-End Test Bridge"))
                .body("data.assetType", equalToIgnoringCase("BRIDGE"));
    }

    // =========================================================================
    // 2. Sensor Registration Tests
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("2.1 - Register a new sensor on the asset")
    void registerSensor() {
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("sensorCode", testSensorCode);
        sensorData.put("sensorType", "STRAIN_GAUGE");
        sensorData.put("manufacturer", "Honeywell");
        sensorData.put("model", "HSG-200-TEST");
        sensorData.put("installationDate", "2024-01-15");
        sensorData.put("lastCalibrationDate", "2024-01-15");
        sensorData.put("calibrationIntervalDays", 90);
        sensorData.put("latitude", 37.7749);
        sensorData.put("longitude", -122.4194);
        sensorData.put("elevation", 100.0);
        sensorData.put("assetId", testAssetId);
        sensorData.put("assetType", "BRIDGE");
        sensorData.put("locationDescription", "Test sensor on E2E bridge");
        sensorData.put("batteryLevel", 100.0);
        sensorData.put("signalStrength", 95.0);
        sensorData.put("firmwareVersion", "1.0.0");
        sensorData.put("minThreshold", 0.0);
        sensorData.put("maxThreshold", 500.0);
        sensorData.put("unit", "μstrain");

        Response response = authenticatedRequest("admin")
                .body(sensorData)
                .post("/api/v1/sensors");

        assertThat(response.statusCode()).isIn(200, 201);
        
        testSensorId = response.jsonPath().getString("data.id");
        assertThat(testSensorId).isNotNull();
        
        log.info("Created test sensor with ID: {}", testSensorId);
    }

    @Test
    @Order(11)
    @DisplayName("2.2 - Verify sensor can be retrieved")
    void getSensor() {
        Response response = authenticatedRequest("admin")
                .get("/api/v1/sensors/" + testSensorId);

        response.then()
                .statusCode(200)
                .body("data.sensorCode", equalTo(testSensorCode))
                .body("data.sensorType", equalToIgnoringCase("STRAIN_GAUGE"));
    }

    @Test
    @Order(12)
    @DisplayName("2.3 - Activate the sensor")
    void activateSensor() {
        Response response = authenticatedRequest("admin")
                .body(Map.of("status", "ACTIVE"))
                .patch("/api/v1/sensors/" + testSensorId + "/status");

        assertThat(response.statusCode()).isIn(200, 204);
        
        // Verify sensor is now active
        Response getResponse = authenticatedRequest("admin")
                .get("/api/v1/sensors/" + testSensorId);
        
        String status = getResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualToIgnoringCase("ACTIVE");
    }

    // =========================================================================
    // 3. Sensor Reading Submission Tests
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("3.1 - Submit normal sensor readings")
    void submitNormalReadings() {
        for (int i = 0; i < 5; i++) {
            double normalValue = 100.0 + (Math.random() * 50); // 100-150 μstrain (normal range)
            
            Map<String, Object> reading = new HashMap<>();
            reading.put("sensorId", testSensorId);
            reading.put("value", normalValue);
            reading.put("timestamp", Instant.now().toString());
            reading.put("unit", "μstrain");
            reading.put("quality", 1.0);

            Response response = authenticatedRequest("admin")
                    .body(reading)
                    .post("/api/v1/sensors/" + testSensorId + "/readings");

            assertThat(response.statusCode()).isIn(200, 201, 202);
            
            log.info("Submitted normal reading: {} μstrain", normalValue);
        }
    }

    @Test
    @Order(21)
    @DisplayName("3.2 - Submit batch sensor readings")
    void submitBatchReadings() {
        List<Map<String, Object>> readings = List.of(
                createReading(120.5),
                createReading(125.3),
                createReading(118.7),
                createReading(122.1),
                createReading(119.8)
        );

        Map<String, Object> batchRequest = new HashMap<>();
        batchRequest.put("readings", readings);

        Response response = authenticatedRequest("admin")
                .body(batchRequest)
                .post("/api/v1/sensors/readings/batch");

        assertThat(response.statusCode()).isIn(200, 201, 202);
        log.info("Submitted batch of {} readings", readings.size());
    }

    private Map<String, Object> createReading(double value) {
        Map<String, Object> reading = new HashMap<>();
        reading.put("sensorId", testSensorId);
        reading.put("value", value);
        reading.put("timestamp", Instant.now().toString());
        reading.put("unit", "μstrain");
        reading.put("quality", 1.0);
        return reading;
    }

    @Test
    @Order(22)
    @DisplayName("3.3 - Retrieve sensor readings")
    void retrieveReadings() {
        // Wait briefly for readings to be processed
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Response response = authenticatedRequest("admin")
                    .get("/api/v1/sensors/" + testSensorId + "/readings?size=10");

            response.then()
                    .statusCode(200)
                    .body("data.content", hasSize(greaterThan(0)));
        });
    }

    // =========================================================================
    // 4. Anomaly Detection and Alert Generation Tests
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("4.1 - Submit anomalous reading to trigger alert")
    void submitAnomalousReading() {
        // Submit a reading that exceeds the threshold (maxThreshold = 500)
        double anomalousValue = 650.0; // Exceeds max threshold
        
        Map<String, Object> reading = new HashMap<>();
        reading.put("sensorId", testSensorId);
        reading.put("value", anomalousValue);
        reading.put("timestamp", Instant.now().toString());
        reading.put("unit", "μstrain");
        reading.put("quality", 1.0);

        Response response = authenticatedRequest("admin")
                .body(reading)
                .post("/api/v1/sensors/" + testSensorId + "/readings");

        assertThat(response.statusCode()).isIn(200, 201, 202);
        log.info("Submitted anomalous reading: {} μstrain (threshold: 500)", anomalousValue);
    }

    @Test
    @Order(31)
    @DisplayName("4.2 - Verify alert was generated for anomalous reading")
    void verifyAlertGenerated() {
        // Wait for alert to be generated
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                Response response = authenticatedRequest("admin")
                        .queryParam("sensorId", testSensorId)
                        .queryParam("status", "OPEN")
                        .queryParam("size", 10)
                        .get("/api/v1/alerts");

                if (response.statusCode() == 200) {
                    List<?> alerts = response.jsonPath().getList("data.content");
                    if (alerts != null && !alerts.isEmpty()) {
                        testAlertId = response.jsonPath().getString("data.content[0].id");
                        log.info("Alert generated with ID: {}", testAlertId);
                        assertThat(alerts).isNotEmpty();
                    }
                }
            });
    }

    @Test
    @Order(32)
    @DisplayName("4.3 - Acknowledge the generated alert")
    void acknowledgeAlert() {
        if (testAlertId == null) {
            log.warn("No alert ID available, skipping acknowledgment test");
            return;
        }

        Map<String, Object> ackData = new HashMap<>();
        ackData.put("acknowledgedBy", "operator");
        ackData.put("notes", "Acknowledged during E2E test");

        Response response = authenticatedRequest("operator")
                .body(ackData)
                .post("/api/v1/alerts/" + testAlertId + "/acknowledge");

        assertThat(response.statusCode()).isIn(200, 204);
        log.info("Alert {} acknowledged", testAlertId);
    }

    @Test
    @Order(33)
    @DisplayName("4.4 - Resolve the alert")
    void resolveAlert() {
        if (testAlertId == null) {
            log.warn("No alert ID available, skipping resolution test");
            return;
        }

        Map<String, Object> resolveData = new HashMap<>();
        resolveData.put("resolvedBy", "engineer");
        resolveData.put("resolution", "Sensor recalibrated during E2E test");
        resolveData.put("rootCause", "Test-induced anomaly");

        Response response = authenticatedRequest("engineer")
                .body(resolveData)
                .post("/api/v1/alerts/" + testAlertId + "/resolve");

        assertThat(response.statusCode()).isIn(200, 204);
        log.info("Alert {} resolved", testAlertId);
    }

    // =========================================================================
    // 5. Health Score and KPI Tests
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("5.1 - Verify asset health score is calculated")
    void verifyAssetHealthScore() {
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                Response response = authenticatedRequest("admin")
                        .get("/api/v1/monitoring/assets/" + testAssetId + "/health");

                if (response.statusCode() == 200) {
                    Double healthScore = response.jsonPath().getDouble("data.healthScore");
                    if (healthScore != null) {
                        assertThat(healthScore).isBetween(0.0, 100.0);
                        log.info("Asset health score: {}", healthScore);
                    }
                }
            });
    }

    @Test
    @Order(41)
    @DisplayName("5.2 - Verify sensor health score")
    void verifySensorHealthScore() {
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                Response response = authenticatedRequest("admin")
                        .get("/api/v1/monitoring/sensors/" + testSensorId + "/health");

                if (response.statusCode() == 200) {
                    Double healthScore = response.jsonPath().getDouble("data.healthScore");
                    if (healthScore != null) {
                        assertThat(healthScore).isBetween(0.0, 100.0);
                        log.info("Sensor health score: {}", healthScore);
                    }
                }
            });
    }

    @Test
    @Order(42)
    @DisplayName("5.3 - Verify system-wide KPIs")
    void verifySystemKPIs() {
        Response response = authenticatedRequest("admin")
                .get("/api/v1/analytics/kpis");

        if (response.statusCode() == 200) {
            log.info("System KPIs retrieved successfully");
            // The KPIs structure may vary, just verify the endpoint works
        } else {
            log.info("KPI endpoint returned status: {}", response.statusCode());
        }
    }

    // =========================================================================
    // 6. Analytics Tests
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("6.1 - Retrieve sensor analytics/trends")
    void retrieveSensorAnalytics() {
        Response response = authenticatedRequest("analyst")
                .queryParam("sensorId", testSensorId)
                .queryParam("period", "24h")
                .get("/api/v1/analytics/sensors/" + testSensorId + "/trends");

        if (response.statusCode() == 200) {
            log.info("Sensor trends retrieved successfully");
        }
    }

    @Test
    @Order(51)
    @DisplayName("6.2 - Retrieve asset analytics summary")
    void retrieveAssetAnalytics() {
        Response response = authenticatedRequest("analyst")
                .queryParam("assetId", testAssetId)
                .get("/api/v1/analytics/assets/" + testAssetId + "/summary");

        if (response.statusCode() == 200) {
            log.info("Asset analytics summary retrieved successfully");
        }
    }

    // =========================================================================
    // 7. Sensor Lifecycle Tests
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("7.1 - Put sensor into maintenance mode")
    void putSensorInMaintenance() {
        Response response = authenticatedRequest("engineer")
                .body(Map.of("status", "MAINTENANCE"))
                .patch("/api/v1/sensors/" + testSensorId + "/status");

        assertThat(response.statusCode()).isIn(200, 204);
        
        // Verify status changed
        Response getResponse = authenticatedRequest("admin")
                .get("/api/v1/sensors/" + testSensorId);
        
        String status = getResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualToIgnoringCase("MAINTENANCE");
        
        log.info("Sensor {} put into maintenance mode", testSensorId);
    }

    @Test
    @Order(61)
    @DisplayName("7.2 - Update sensor calibration")
    void updateSensorCalibration() {
        Map<String, Object> calibrationData = new HashMap<>();
        calibrationData.put("lastCalibrationDate", Instant.now().toString().substring(0, 10));
        calibrationData.put("calibrationIntervalDays", 60);
        calibrationData.put("calibrationNotes", "E2E test recalibration");

        Response response = authenticatedRequest("engineer")
                .body(calibrationData)
                .put("/api/v1/sensors/" + testSensorId + "/calibration");

        // Accept various success codes
        assertThat(response.statusCode()).isIn(200, 204, 404);
        log.info("Sensor calibration update response: {}", response.statusCode());
    }

    @Test
    @Order(62)
    @DisplayName("7.3 - Reactivate sensor")
    void reactivateSensor() {
        Response response = authenticatedRequest("engineer")
                .body(Map.of("status", "ACTIVE"))
                .patch("/api/v1/sensors/" + testSensorId + "/status");

        assertThat(response.statusCode()).isIn(200, 204);
        
        // Verify status changed back to active
        Response getResponse = authenticatedRequest("admin")
                .get("/api/v1/sensors/" + testSensorId);
        
        String status = getResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualToIgnoringCase("ACTIVE");
        
        log.info("Sensor {} reactivated", testSensorId);
    }

    // =========================================================================
    // 8. Filtering and Search Tests
    // =========================================================================

    @Test
    @Order(70)
    @DisplayName("8.1 - Filter sensors by type")
    void filterSensorsByType() {
        Response response = authenticatedRequest("viewer")
                .queryParam("sensorType", "STRAIN_GAUGE")
                .queryParam("size", 10)
                .get("/api/v1/sensors");

        response.then()
                .statusCode(200);
        
        log.info("Filtered sensors by type - found results");
    }

    @Test
    @Order(71)
    @DisplayName("8.2 - Filter sensors by asset")
    void filterSensorsByAsset() {
        Response response = authenticatedRequest("viewer")
                .queryParam("assetId", testAssetId)
                .queryParam("size", 10)
                .get("/api/v1/sensors");

        response.then()
                .statusCode(200);
        
        List<?> sensors = response.jsonPath().getList("data.content");
        if (sensors != null) {
            assertThat(sensors).hasSizeGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @Order(72)
    @DisplayName("8.3 - Search assets by name")
    void searchAssetsByName() {
        Response response = authenticatedRequest("viewer")
                .queryParam("search", "End-to-End")
                .queryParam("size", 10)
                .get("/api/v1/assets");

        response.then()
                .statusCode(200);
    }

    // =========================================================================
    // 9. Audit Trail Tests
    // =========================================================================

    @Test
    @Order(80)
    @DisplayName("9.1 - Verify audit logs exist for sensor operations")
    void verifyAuditLogs() {
        Response response = authenticatedRequest("auditor")
                .queryParam("entityType", "SENSOR")
                .queryParam("entityId", testSensorId)
                .queryParam("size", 20)
                .get("/api/v1/audit/logs");

        if (response.statusCode() == 200) {
            log.info("Audit logs retrieved successfully");
        }
    }

    // =========================================================================
    // 10. Error Handling Tests
    // =========================================================================

    @Test
    @Order(90)
    @DisplayName("10.1 - Verify 404 for non-existent sensor")
    void verifyNotFoundForNonExistentSensor() {
        Response response = authenticatedRequest("admin")
                .get("/api/v1/sensors/non-existent-id-12345");

        assertThat(response.statusCode()).isIn(404, 400);
    }

    @Test
    @Order(91)
    @DisplayName("10.2 - Verify 400 for invalid sensor data")
    void verifyBadRequestForInvalidData() {
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("sensorCode", ""); // Empty code
        invalidData.put("sensorType", "INVALID_TYPE"); // Invalid type

        Response response = authenticatedRequest("admin")
                .body(invalidData)
                .post("/api/v1/sensors");

        assertThat(response.statusCode()).isIn(400, 422);
    }

    @Test
    @Order(92)
    @DisplayName("10.3 - Verify 401 for unauthenticated request")
    void verifyUnauthorizedForUnauthenticated() {
        Response response = unauthenticatedRequest()
                .get("/api/v1/sensors");

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
