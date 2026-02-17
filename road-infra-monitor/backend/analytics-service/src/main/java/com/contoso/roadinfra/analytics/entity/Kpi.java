package com.contoso.roadinfra.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "kpis", indexes = {
        @Index(name = "idx_kpi_metric_name", columnList = "metric_name"),
        @Index(name = "idx_kpi_asset_id", columnList = "asset_id"),
        @Index(name = "idx_kpi_period", columnList = "period"),
        @Index(name = "idx_kpi_calculated_at", columnList = "calculated_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Kpi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "display_name")
    private String displayName;

    private String category;

    private Double value;

    @Column(name = "previous_value")
    private Double previousValue;

    @Column(name = "target_value")
    private Double targetValue;

    private String unit;

    @Column(name = "percentage_change")
    private Double percentageChange;

    private String trend;

    @Column(name = "on_target")
    private Boolean onTarget;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "asset_name")
    private String assetName;

    private String period;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @ElementCollection
    @CollectionTable(name = "kpi_historical_data", joinColumns = @JoinColumn(name = "kpi_id"))
    @MapKeyColumn(name = "data_point")
    @Column(name = "data_value")
    @Builder.Default
    private Map<String, Double> historicalData = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "kpi_breakdown", joinColumns = @JoinColumn(name = "kpi_id"))
    @MapKeyColumn(name = "breakdown_key")
    @Column(name = "breakdown_value")
    @Builder.Default
    private Map<String, Double> breakdown = new HashMap<>();

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
