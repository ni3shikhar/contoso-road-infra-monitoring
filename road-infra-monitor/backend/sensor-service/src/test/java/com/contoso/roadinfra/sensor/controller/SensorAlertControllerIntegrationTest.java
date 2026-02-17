package com.contoso.roadinfra.sensor.controller;

import com.contoso.roadinfra.common.enums.AlertSeverity;
import com.contoso.roadinfra.common.enums.SensorType;
import com.contoso.roadinfra.sensor.config.TestContainerConfig;
import com.contoso.roadinfra.sensor.config.TestSecurityConfig;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.entity.SensorAlert;
import com.contoso.roadinfra.sensor.enums.SensorAlertType;
import com.contoso.roadinfra.sensor.enums.SensorStatus;
import com.contoso.roadinfra.sensor.repository.SensorAlertRepository;
import com.contoso.roadinfra.sensor.repository.SensorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({TestContainerConfig.class, TestSecurityConfig.class})
class SensorAlertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorAlertRepository alertRepository;

    private static final String BASE_URL = "/api/v1/alerts";
    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final String OPERATOR_TOKEN = "Bearer operator-token";
    private static final String VIEWER_TOKEN = "Bearer viewer-token";
    private static final String TECHNICIAN_TOKEN = "Bearer technician-token";

    private Sensor testSensor;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        sensorRepository.deleteAll();

        testSensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
    }

    private Sensor createTestSensor(String sensorCode, SensorType type) {
        Sensor sensor = new Sensor();
        sensor.setSensorCode(sensorCode);
        sensor.setSensorType(type);
        sensor.setManufacturer("TestCorp");
        sensor.setModel("Model-X");
        sensor.setInstallationDate(LocalDate.now().minusMonths(6));
        sensor.setLastCalibrationDate(LocalDate.now().minusMonths(1));
        sensor.setCalibrationIntervalDays(90);
        sensor.setStatus(SensorStatus.ACTIVE);
        sensor.setLatitude(37.7749);
        sensor.setLongitude(-122.4194);
        sensor.setAssetId(UUID.randomUUID());
        sensor.setAssetType("BRIDGE");
        sensor.setBatteryLevel(85);
        sensor.setSignalStrength(-60);
        sensor.setMinThreshold(0.0);
        sensor.setMaxThreshold(100.0);
        sensor.setCreatedBy("test-user");
        return sensorRepository.save(sensor);
    }

    private SensorAlert createTestAlert(UUID sensorId, SensorAlertType alertType, 
                                        AlertSeverity severity, boolean acknowledged) {
        SensorAlert alert = new SensorAlert();
        alert.setSensorId(sensorId);
        alert.setAlertType(alertType);
        alert.setMessage("Test alert message");
        alert.setSeverity(severity);
        alert.setAcknowledged(acknowledged);
        alert.setReadingValue(150.0);
        alert.setThresholdValue(100.0);
        return alertRepository.save(alert);
    }

    @Nested
    @DisplayName("GET /api/v1/alerts")
    class GetAllAlerts {

        @Test
        @DisplayName("Should return all alerts for viewer")
        void shouldReturnAllAlertsForViewer() throws Exception {
            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, false);

            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("Should filter alerts by severity")
        void shouldFilterAlertsBySeverity() throws Exception {
            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, false);

            mockMvc.perform(get(BASE_URL)
                            .param("severity", "HIGH")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].severity", is("HIGH")));
        }

        @Test
        @DisplayName("Should filter alerts by acknowledged status")
        void shouldFilterAlertsByAcknowledged() throws Exception {
            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, true);

            mockMvc.perform(get(BASE_URL)
                            .param("acknowledged", "false")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].acknowledged", is(false)));
        }

        @Test
        @DisplayName("Should filter alerts by type")
        void shouldFilterAlertsByType() throws Exception {
            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, false);

            mockMvc.perform(get(BASE_URL)
                            .param("alertType", "THRESHOLD_BREACH")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].alertType", is("THRESHOLD_BREACH")));
        }

        @Test
        @DisplayName("Should return 401 without authorization")
        void shouldReturn401WithoutAuthorization() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/alerts/{id}")
    class GetAlertById {

        @Test
        @DisplayName("Should return alert by ID")
        void shouldReturnAlertById() throws Exception {
            SensorAlert alert = createTestAlert(testSensor.getId(), 
                    SensorAlertType.THRESHOLD_BREACH, AlertSeverity.HIGH, false);

            mockMvc.perform(get(BASE_URL + "/" + alert.getId())
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(alert.getId().toString())))
                    .andExpect(jsonPath("$.alertType", is("THRESHOLD_BREACH")))
                    .andExpect(jsonPath("$.severity", is("HIGH")));
        }

        @Test
        @DisplayName("Should return 404 for non-existent alert")
        void shouldReturn404ForNonExistentAlert() throws Exception {
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/" + randomId)
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/alerts/sensor/{sensorId}")
    class GetAlertsBySensor {

        @Test
        @DisplayName("Should return alerts for specific sensor")
        void shouldReturnAlertsForSpecificSensor() throws Exception {
            Sensor sensor2 = createTestSensor("AC-TN-002", SensorType.ACCELEROMETER);

            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(sensor2.getId(), SensorAlertType.OFFLINE, 
                    AlertSeverity.HIGH, false);

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId())
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].sensorId", 
                            is(testSensor.getId().toString())));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/alerts/unacknowledged")
    class GetUnacknowledgedAlerts {

        @Test
        @DisplayName("Should return only unacknowledged alerts")
        void shouldReturnOnlyUnacknowledgedAlerts() throws Exception {
            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, true);

            mockMvc.perform(get(BASE_URL + "/unacknowledged")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].acknowledged", is(false)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/alerts/{id}/acknowledge")
    class AcknowledgeAlert {

        @Test
        @DisplayName("Should acknowledge alert as operator")
        void shouldAcknowledgeAlertAsOperator() throws Exception {
            SensorAlert alert = createTestAlert(testSensor.getId(), 
                    SensorAlertType.THRESHOLD_BREACH, AlertSeverity.HIGH, false);

            mockMvc.perform(patch(BASE_URL + "/" + alert.getId() + "/acknowledge")
                            .header("Authorization", OPERATOR_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acknowledged", is(true)))
                    .andExpect(jsonPath("$.acknowledgedBy", notNullValue()))
                    .andExpect(jsonPath("$.acknowledgedAt", notNullValue()));

            // Verify persisted
            SensorAlert updated = alertRepository.findById(alert.getId()).orElseThrow();
            assertThat(updated.isAcknowledged()).isTrue();
        }

        @Test
        @DisplayName("Should acknowledge alert as technician")
        void shouldAcknowledgeAlertAsTechnician() throws Exception {
            SensorAlert alert = createTestAlert(testSensor.getId(), 
                    SensorAlertType.THRESHOLD_BREACH, AlertSeverity.HIGH, false);

            mockMvc.perform(patch(BASE_URL + "/" + alert.getId() + "/acknowledge")
                            .header("Authorization", TECHNICIAN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acknowledged", is(true)));
        }

        @Test
        @DisplayName("Should return 403 for viewer trying to acknowledge")
        void shouldReturn403ForViewerTryingToAcknowledge() throws Exception {
            SensorAlert alert = createTestAlert(testSensor.getId(), 
                    SensorAlertType.THRESHOLD_BREACH, AlertSeverity.HIGH, false);

            mockMvc.perform(patch(BASE_URL + "/" + alert.getId() + "/acknowledge")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 for non-existent alert")
        void shouldReturn404ForNonExistentAlert() throws Exception {
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(patch(BASE_URL + "/" + randomId + "/acknowledge")
                            .header("Authorization", OPERATOR_TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/alerts/acknowledge-batch")
    class BatchAcknowledge {

        @Test
        @DisplayName("Should acknowledge multiple alerts")
        void shouldAcknowledgeMultipleAlerts() throws Exception {
            SensorAlert alert1 = createTestAlert(testSensor.getId(), 
                    SensorAlertType.THRESHOLD_BREACH, AlertSeverity.HIGH, false);
            SensorAlert alert2 = createTestAlert(testSensor.getId(), 
                    SensorAlertType.LOW_BATTERY, AlertSeverity.MEDIUM, false);
            
            List<UUID> alertIds = List.of(alert1.getId(), alert2.getId());

            mockMvc.perform(post(BASE_URL + "/acknowledge-batch")
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(alertIds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(2)));

            // Verify all acknowledged
            assertThat(alertRepository.findById(alert1.getId()).orElseThrow().isAcknowledged())
                    .isTrue();
            assertThat(alertRepository.findById(alert2.getId()).orElseThrow().isAcknowledged())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/alerts/count/unacknowledged")
    class GetUnacknowledgedCount {

        @Test
        @DisplayName("Should return count of unacknowledged alerts")
        void shouldReturnCountOfUnacknowledgedAlerts() throws Exception {
            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.OFFLINE, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, true);

            mockMvc.perform(get(BASE_URL + "/count/unacknowledged")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/alerts/sensor/{sensorId}/count/unacknowledged")
    class GetUnacknowledgedCountBySensor {

        @Test
        @DisplayName("Should return count for specific sensor")
        void shouldReturnCountForSpecificSensor() throws Exception {
            Sensor sensor2 = createTestSensor("AC-TN-002", SensorType.ACCELEROMETER);

            createTestAlert(testSensor.getId(), SensorAlertType.THRESHOLD_BREACH, 
                    AlertSeverity.HIGH, false);
            createTestAlert(testSensor.getId(), SensorAlertType.LOW_BATTERY, 
                    AlertSeverity.MEDIUM, false);
            createTestAlert(sensor2.getId(), SensorAlertType.OFFLINE, 
                    AlertSeverity.HIGH, false);

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId() + "/count/unacknowledged")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2"));
        }
    }
}
