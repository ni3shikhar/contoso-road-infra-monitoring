package com.contoso.roadinfra.sensor.controller;

import com.contoso.roadinfra.common.enums.SensorType;
import com.contoso.roadinfra.sensor.config.TestContainerConfig;
import com.contoso.roadinfra.sensor.config.TestSecurityConfig;
import com.contoso.roadinfra.sensor.dto.SensorCreateRequest;
import com.contoso.roadinfra.sensor.dto.SensorStatusUpdateRequest;
import com.contoso.roadinfra.sensor.dto.SensorUpdateRequest;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.enums.SensorStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
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
class SensorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensorRepository sensorRepository;

    private static final String BASE_URL = "/api/v1/sensors";
    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final String OPERATOR_TOKEN = "Bearer operator-token";
    private static final String VIEWER_TOKEN = "Bearer viewer-token";
    private static final String TECHNICIAN_TOKEN = "Bearer technician-token";

    @BeforeEach
    void setUp() {
        sensorRepository.deleteAll();
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

    private SensorCreateRequest createSensorRequest(String sensorCode, SensorType type) {
        SensorCreateRequest request = new SensorCreateRequest();
        request.setSensorCode(sensorCode);
        request.setSensorType(type);
        request.setManufacturer("TestCorp");
        request.setModel("Model-X");
        request.setInstallationDate(LocalDate.now());
        request.setCalibrationIntervalDays(90);
        request.setLatitude(37.7749);
        request.setLongitude(-122.4194);
        request.setAssetId(UUID.randomUUID());
        request.setAssetType("BRIDGE");
        request.setMinThreshold(0.0);
        request.setMaxThreshold(100.0);
        return request;
    }

    @Nested
    @DisplayName("GET /api/v1/sensors")
    class GetAllSensors {

        @Test
        @DisplayName("Should return sensors list for viewer")
        void shouldReturnSensorsForViewer() throws Exception {
            createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            createTestSensor("AC-TN-002", SensorType.ACCELEROMETER);

            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].sensorCode", 
                            containsInAnyOrder("SG-BR-001", "AC-TN-002")));
        }

        @Test
        @DisplayName("Should filter sensors by type")
        void shouldFilterSensorsByType() throws Exception {
            createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            createTestSensor("AC-TN-002", SensorType.ACCELEROMETER);

            mockMvc.perform(get(BASE_URL)
                            .param("type", "STRAIN_GAUGE")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].sensorType", is("STRAIN_GAUGE")));
        }

        @Test
        @DisplayName("Should filter sensors by status")
        void shouldFilterSensorsByStatus() throws Exception {
            Sensor active = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            Sensor inactive = createTestSensor("AC-TN-002", SensorType.ACCELEROMETER);
            inactive.setStatus(SensorStatus.INACTIVE);
            sensorRepository.save(inactive);

            mockMvc.perform(get(BASE_URL)
                            .param("status", "ACTIVE")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 401 without token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sensors/{id}")
    class GetSensorById {

        @Test
        @DisplayName("Should return sensor by ID")
        void shouldReturnSensorById() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            mockMvc.perform(get(BASE_URL + "/" + sensor.getId())
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(sensor.getId().toString())))
                    .andExpect(jsonPath("$.sensorCode", is("SG-BR-001")))
                    .andExpect(jsonPath("$.sensorType", is("STRAIN_GAUGE")));
        }

        @Test
        @DisplayName("Should return 404 for non-existent sensor")
        void shouldReturn404ForNonExistentSensor() throws Exception {
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/" + randomId)
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sensors")
    class CreateSensor {

        @Test
        @DisplayName("Should create sensor as admin")
        void shouldCreateSensorAsAdmin() throws Exception {
            SensorCreateRequest request = createSensorRequest("SG-BR-001", SensorType.STRAIN_GAUGE);

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .header("Authorization", ADMIN_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sensorCode", is("SG-BR-001")))
                    .andExpect(jsonPath("$.sensorType", is("STRAIN_GAUGE")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andReturn();

            // Verify it was persisted
            assertThat(sensorRepository.findBySensorCode("SG-BR-001")).isPresent();
        }

        @Test
        @DisplayName("Should create sensor as operator")
        void shouldCreateSensorAsOperator() throws Exception {
            SensorCreateRequest request = createSensorRequest("SG-BR-002", SensorType.STRAIN_GAUGE);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 403 for viewer trying to create")
        void shouldReturn403ForViewerTryingToCreate() throws Exception {
            SensorCreateRequest request = createSensorRequest("SG-BR-003", SensorType.STRAIN_GAUGE);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", VIEWER_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 for invalid sensor code format")
        void shouldReturn400ForInvalidSensorCode() throws Exception {
            SensorCreateRequest request = createSensorRequest("INVALID-CODE", SensorType.STRAIN_GAUGE);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", ADMIN_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 for duplicate sensor code")
        void shouldReturn409ForDuplicateSensorCode() throws Exception {
            createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            SensorCreateRequest request = createSensorRequest("SG-BR-001", SensorType.STRAIN_GAUGE);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", ADMIN_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/sensors/{id}")
    class UpdateSensor {

        @Test
        @DisplayName("Should update sensor as admin")
        void shouldUpdateSensorAsAdmin() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            SensorUpdateRequest request = new SensorUpdateRequest();
            request.setManufacturer("UpdatedCorp");
            request.setModel("Model-Y");

            mockMvc.perform(put(BASE_URL + "/" + sensor.getId())
                            .header("Authorization", ADMIN_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.manufacturer", is("UpdatedCorp")))
                    .andExpect(jsonPath("$.model", is("Model-Y")));
        }

        @Test
        @DisplayName("Should return 404 for non-existent sensor")
        void shouldReturn404ForNonExistentSensor() throws Exception {
            UUID randomId = UUID.randomUUID();
            SensorUpdateRequest request = new SensorUpdateRequest();
            request.setManufacturer("UpdatedCorp");

            mockMvc.perform(put(BASE_URL + "/" + randomId)
                            .header("Authorization", ADMIN_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sensors/{id}")
    class DeleteSensor {

        @Test
        @DisplayName("Should delete sensor as admin")
        void shouldDeleteSensorAsAdmin() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            mockMvc.perform(delete(BASE_URL + "/" + sensor.getId())
                            .header("Authorization", ADMIN_TOKEN))
                    .andExpect(status().isNoContent());

            assertThat(sensorRepository.findById(sensor.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should return 403 for operator trying to delete")
        void shouldReturn403ForOperatorTryingToDelete() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            mockMvc.perform(delete(BASE_URL + "/" + sensor.getId())
                            .header("Authorization", OPERATOR_TOKEN))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for viewer trying to delete")
        void shouldReturn403ForViewerTryingToDelete() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            mockMvc.perform(delete(BASE_URL + "/" + sensor.getId())
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sensors/{id}/status")
    class UpdateSensorStatus {

        @Test
        @DisplayName("Should update sensor status as operator")
        void shouldUpdateSensorStatusAsOperator() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            SensorStatusUpdateRequest request = new SensorStatusUpdateRequest();
            request.setStatus(SensorStatus.MAINTENANCE);
            request.setReason("Scheduled maintenance");

            mockMvc.perform(patch(BASE_URL + "/" + sensor.getId() + "/status")
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("MAINTENANCE")));

            // Verify the change persisted
            Sensor updated = sensorRepository.findById(sensor.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(SensorStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("Should return 403 for viewer trying to update status")
        void shouldReturn403ForViewerTryingToUpdateStatus() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            SensorStatusUpdateRequest request = new SensorStatusUpdateRequest();
            request.setStatus(SensorStatus.MAINTENANCE);

            mockMvc.perform(patch(BASE_URL + "/" + sensor.getId() + "/status")
                            .header("Authorization", VIEWER_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sensors/statistics")
    class GetStatistics {

        @Test
        @DisplayName("Should return counts by type")
        void shouldReturnCountsByType() throws Exception {
            createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            createTestSensor("SG-BR-002", SensorType.STRAIN_GAUGE);
            createTestSensor("AC-TN-001", SensorType.ACCELEROMETER);

            mockMvc.perform(get(BASE_URL + "/statistics/by-type")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should return counts by status")
        void shouldReturnCountsByStatus() throws Exception {
            Sensor active = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            Sensor maintenance = createTestSensor("SG-BR-002", SensorType.STRAIN_GAUGE);
            maintenance.setStatus(SensorStatus.MAINTENANCE);
            sensorRepository.save(maintenance);

            mockMvc.perform(get(BASE_URL + "/statistics/by-status")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sensors/{id}/calibrate")
    class CalibrateSensor {

        @Test
        @DisplayName("Should update calibration date as technician")
        void shouldUpdateCalibrationDateAsTechnician() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);
            LocalDate oldCalibrationDate = sensor.getLastCalibrationDate();

            mockMvc.perform(patch(BASE_URL + "/" + sensor.getId() + "/calibrate")
                            .header("Authorization", TECHNICIAN_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastCalibrationDate", not(is(oldCalibrationDate.toString()))));

            Sensor updated = sensorRepository.findById(sensor.getId()).orElseThrow();
            assertThat(updated.getLastCalibrationDate()).isAfter(oldCalibrationDate);
        }

        @Test
        @DisplayName("Should return 403 for viewer trying to calibrate")
        void shouldReturn403ForViewerTryingToCalibrate() throws Exception {
            Sensor sensor = createTestSensor("SG-BR-001", SensorType.STRAIN_GAUGE);

            mockMvc.perform(patch(BASE_URL + "/" + sensor.getId() + "/calibrate")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isForbidden());
        }
    }
}
