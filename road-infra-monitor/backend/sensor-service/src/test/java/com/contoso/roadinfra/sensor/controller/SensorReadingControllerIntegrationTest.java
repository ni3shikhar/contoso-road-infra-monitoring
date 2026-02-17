package com.contoso.roadinfra.sensor.controller;

import com.contoso.roadinfra.common.enums.SensorType;
import com.contoso.roadinfra.sensor.config.TestContainerConfig;
import com.contoso.roadinfra.sensor.config.TestSecurityConfig;
import com.contoso.roadinfra.common.constants.DataQuality;
import com.contoso.roadinfra.sensor.dto.BatchReadingRequest;
import com.contoso.roadinfra.sensor.dto.SensorReadingRequest;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.entity.SensorReading;
import com.contoso.roadinfra.sensor.enums.SensorStatus;
import com.contoso.roadinfra.sensor.repository.SensorReadingRepository;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
class SensorReadingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorReadingRepository readingRepository;

    private static final String BASE_URL = "/api/v1/readings";
    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final String OPERATOR_TOKEN = "Bearer operator-token";
    private static final String VIEWER_TOKEN = "Bearer viewer-token";

    private Sensor testSensor;

    @BeforeEach
    void setUp() {
        readingRepository.deleteAll();
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

    private SensorReading createTestReading(UUID sensorId, double value, Instant timestamp) {
        SensorReading reading = new SensorReading();
        reading.setSensorId(sensorId);
        reading.setValue(value);
        reading.setUnit("μɛ");
        reading.setTimestamp(timestamp);
        reading.setQuality(DataQuality.GOOD);
        reading.setAnomaly(false);
        return readingRepository.save(reading);
    }

    @Nested
    @DisplayName("POST /api/v1/readings")
    class IngestReading {

        @Test
        @DisplayName("Should ingest reading as operator")
        void shouldIngestReadingAsOperator() throws Exception {
            SensorReadingRequest request = new SensorReadingRequest();
            request.setSensorId(testSensor.getId());
            request.setValue(50.5);
            request.setUnit("μɛ");
            request.setTimestamp(Instant.now());
            request.setQuality(DataQuality.GOOD);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sensorId", is(testSensor.getId().toString())))
                    .andExpect(jsonPath("$.value", is(50.5)))
                    .andExpect(jsonPath("$.anomaly", is(false)));

            // Verify persisted
            List<SensorReading> readings = readingRepository.findBySensorId(testSensor.getId());
            assertThat(readings).hasSize(1);
            assertThat(readings.get(0).getValue()).isEqualTo(50.5);
        }

        @Test
        @DisplayName("Should mark reading as anomaly when exceeding threshold")
        void shouldMarkReadingAsAnomalyWhenExceedingThreshold() throws Exception {
            SensorReadingRequest request = new SensorReadingRequest();
            request.setSensorId(testSensor.getId());
            request.setValue(150.0); // Exceeds maxThreshold of 100
            request.setUnit("μɛ");
            request.setTimestamp(Instant.now());
            request.setQuality(DataQuality.GOOD);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.anomaly", is(true)))
                    .andExpect(jsonPath("$.anomalyScore", greaterThan(0.0)));
        }

        @Test
        @DisplayName("Should return 403 for viewer trying to ingest")
        void shouldReturn403ForViewerTryingToIngest() throws Exception {
            SensorReadingRequest request = new SensorReadingRequest();
            request.setSensorId(testSensor.getId());
            request.setValue(50.5);
            request.setUnit("μɛ");
            request.setTimestamp(Instant.now());

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", VIEWER_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            SensorReadingRequest request = new SensorReadingRequest();
            // Missing sensorId and value

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/readings/batch")
    class BatchIngest {

        @Test
        @DisplayName("Should ingest batch readings")
        void shouldIngestBatchReadings() throws Exception {
            Instant now = Instant.now();
            
            BatchReadingRequest.BatchReadingItem reading1 = BatchReadingRequest.BatchReadingItem.builder()
                    .sensorId(testSensor.getId())
                    .value(50.0)
                    .unit("μɛ")
                    .timestamp(now.minusSeconds(10))
                    .quality(DataQuality.GOOD)
                    .build();

            BatchReadingRequest.BatchReadingItem reading2 = BatchReadingRequest.BatchReadingItem.builder()
                    .sensorId(testSensor.getId())
                    .value(51.0)
                    .unit("μɛ")
                    .timestamp(now)
                    .quality(DataQuality.GOOD)
                    .build();

            BatchReadingRequest batch = new BatchReadingRequest();
            batch.setReadings(List.of(reading1, reading2));

            mockMvc.perform(post(BASE_URL + "/batch")
                            .header("Authorization", OPERATOR_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(batch)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(2)));

            List<SensorReading> readings = readingRepository.findBySensorId(testSensor.getId());
            assertThat(readings).hasSize(2);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/readings/sensor/{sensorId}")
    class GetReadingsBySensor {

        @Test
        @DisplayName("Should return readings for sensor")
        void shouldReturnReadingsForSensor() throws Exception {
            Instant now = Instant.now();
            createTestReading(testSensor.getId(), 50.0, now.minusSeconds(60));
            createTestReading(testSensor.getId(), 51.0, now.minusSeconds(30));
            createTestReading(testSensor.getId(), 52.0, now);

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId())
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)));
        }

        @Test
        @DisplayName("Should filter readings by time range")
        void shouldFilterReadingsByTimeRange() throws Exception {
            Instant now = Instant.now();
            createTestReading(testSensor.getId(), 50.0, now.minus(2, ChronoUnit.HOURS));
            createTestReading(testSensor.getId(), 51.0, now.minus(30, ChronoUnit.MINUTES));
            createTestReading(testSensor.getId(), 52.0, now);

            String startTime = now.minus(1, ChronoUnit.HOURS).toString();
            String endTime = now.plus(1, ChronoUnit.MINUTES).toString();

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId())
                            .param("startTime", startTime)
                            .param("endTime", endTime)
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/readings/sensor/{sensorId}/latest")
    class GetLatestReading {

        @Test
        @DisplayName("Should return latest reading")
        void shouldReturnLatestReading() throws Exception {
            Instant now = Instant.now();
            createTestReading(testSensor.getId(), 50.0, now.minusSeconds(60));
            createTestReading(testSensor.getId(), 51.0, now.minusSeconds(30));
            createTestReading(testSensor.getId(), 52.0, now);

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId() + "/latest")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.value", is(52.0)));
        }

        @Test
        @DisplayName("Should return 404 when no readings exist")
        void shouldReturn404WhenNoReadingsExist() throws Exception {
            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId() + "/latest")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/readings/sensor/{sensorId}/stats")
    class GetReadingStats {

        @Test
        @DisplayName("Should return reading statistics")
        void shouldReturnReadingStatistics() throws Exception {
            Instant now = Instant.now();
            createTestReading(testSensor.getId(), 50.0, now.minusSeconds(60));
            createTestReading(testSensor.getId(), 60.0, now.minusSeconds(30));
            createTestReading(testSensor.getId(), 70.0, now);

            String startTime = now.minus(2, ChronoUnit.MINUTES).toString();
            String endTime = now.plus(1, ChronoUnit.MINUTES).toString();

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId() + "/stats")
                            .param("startTime", startTime)
                            .param("endTime", endTime)
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(3)))
                    .andExpect(jsonPath("$.min", is(50.0)))
                    .andExpect(jsonPath("$.max", is(70.0)))
                    .andExpect(jsonPath("$.avg", is(60.0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/readings/sensor/{sensorId}/anomalies")
    class GetAnomalies {

        @Test
        @DisplayName("Should return only anomalies")
        void shouldReturnOnlyAnomalies() throws Exception {
            Instant now = Instant.now();
            
            // Normal reading
            createTestReading(testSensor.getId(), 50.0, now.minusSeconds(60));
            
            // Anomaly reading
            SensorReading anomaly = new SensorReading();
            anomaly.setSensorId(testSensor.getId());
            anomaly.setValue(150.0);
            anomaly.setUnit("μɛ");
            anomaly.setTimestamp(now);
            anomaly.setQuality(DataQuality.GOOD);
            anomaly.setAnomaly(true);
            anomaly.setAnomalyScore(0.8);
            readingRepository.save(anomaly);

            mockMvc.perform(get(BASE_URL + "/sensor/" + testSensor.getId() + "/anomalies")
                            .header("Authorization", VIEWER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].anomaly", is(true)));
        }
    }
}
