package com.contoso.roadinfra.sensor.repository;

import com.contoso.roadinfra.sensor.entity.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    Page<SensorReading> findBySensorIdOrderByTimestampDesc(UUID sensorId, Pageable pageable);

    List<SensorReading> findBySensorIdAndTimestampBetweenOrderByTimestampDesc(UUID sensorId,
                                                          Instant start,
                                                          Instant end);

    @Query("SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId ORDER BY sr.timestamp DESC LIMIT 1")
    Optional<SensorReading> findLatestBySensorId(@Param("sensorId") UUID sensorId);

    @Query("SELECT AVG(sr.value) FROM SensorReading sr WHERE sr.sensorId = :sensorId " +
            "AND sr.timestamp BETWEEN :start AND :end")
    Double calculateAverageValue(@Param("sensorId") UUID sensorId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end);

    @Query("SELECT MAX(sr.value) FROM SensorReading sr WHERE sr.sensorId = :sensorId " +
            "AND sr.timestamp BETWEEN :start AND :end")
    Double findMaxValue(@Param("sensorId") UUID sensorId,
                         @Param("start") Instant start,
                         @Param("end") Instant end);

    @Query("SELECT MIN(sr.value) FROM SensorReading sr WHERE sr.sensorId = :sensorId " +
            "AND sr.timestamp BETWEEN :start AND :end")
    Double findMinValue(@Param("sensorId") UUID sensorId,
                         @Param("start") Instant start,
                         @Param("end") Instant end);

    @Query("SELECT STDDEV(sr.value) FROM SensorReading sr WHERE sr.sensorId = :sensorId " +
            "AND sr.timestamp BETWEEN :start AND :end")
    Double calculateStdDeviation(@Param("sensorId") UUID sensorId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end);

    List<SensorReading> findBySensorIdAndAnomalyTrue(UUID sensorId);

    @Query("SELECT COUNT(sr) FROM SensorReading sr WHERE sr.sensorId = :sensorId " +
            "AND sr.timestamp BETWEEN :start AND :end")
    Long countReadingsInPeriod(@Param("sensorId") UUID sensorId,
                               @Param("start") Instant start,
                               @Param("end") Instant end);

    @Query("SELECT COUNT(sr) FROM SensorReading sr WHERE sr.sensorId = :sensorId " +
            "AND sr.timestamp BETWEEN :start AND :end AND sr.anomaly = true")
    Long countAnomaliesInPeriod(@Param("sensorId") UUID sensorId,
                                @Param("start") Instant start,
                                @Param("end") Instant end);

    // Delete old readings for data retention
    @Query("DELETE FROM SensorReading sr WHERE sr.timestamp < :before")
    void deleteReadingsOlderThan(@Param("before") Instant before);
}
