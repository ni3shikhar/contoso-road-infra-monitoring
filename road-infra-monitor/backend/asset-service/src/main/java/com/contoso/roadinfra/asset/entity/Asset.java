package com.contoso.roadinfra.asset.entity;

import com.contoso.roadinfra.asset.constants.ConstructionStatus;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a physical infrastructure asset on the 2km road corridor.
 * Assets can be hierarchical (e.g., a bridge can have piers, deck, and abutment as children).
 */
@Entity
@Table(name = "assets", indexes = {
        @Index(name = "idx_asset_code", columnList = "asset_code", unique = true),
        @Index(name = "idx_asset_type", columnList = "asset_type"),
        @Index(name = "idx_asset_status", columnList = "status"),
        @Index(name = "idx_asset_health", columnList = "health_status"),
        @Index(name = "idx_asset_parent", columnList = "parent_asset_id"),
        @Index(name = "idx_asset_chainage", columnList = "start_chainage, end_chainage")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_code", nullable = false, unique = true, length = 50)
    private String assetCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType assetType;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Chainage values (distance in meters from corridor start)
    @Column(name = "start_chainage", nullable = false)
    private Double startChainage;

    @Column(name = "end_chainage", nullable = false)
    private Double endChainage;

    @Column(name = "length")
    private Double length;

    // Geographic coordinates
    @Column(name = "start_latitude", nullable = false)
    private Double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private Double startLongitude;

    @Column(name = "end_latitude")
    private Double endLatitude;

    @Column(name = "end_longitude")
    private Double endLongitude;

    // Construction dates
    @Column(name = "construction_start_date")
    private LocalDate constructionStartDate;

    @Column(name = "construction_end_date")
    private LocalDate constructionEndDate;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Double completionPercentage = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ConstructionStatus status = ConstructionStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "design_life_years")
    private Integer designLifeYears;

    // Inspection tracking
    @Column(name = "last_inspection_date")
    private LocalDate lastInspectionDate;

    @Column(name = "next_inspection_date")
    private LocalDate nextInspectionDate;

    // Hierarchical relationship
    @Column(name = "parent_asset_id")
    private UUID parentAssetId;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssetInspection> inspections = new ArrayList<>();

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConstructionMilestone> milestones = new ArrayList<>();

    // Flexible metadata as JSON (e.g., bridge type, tunnel lining material, road surface type)
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Audit fields
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Calculate the length from chainage values if not set.
     */
    @PrePersist
    @PreUpdate
    public void calculateLength() {
        if (this.length == null && this.startChainage != null && this.endChainage != null) {
            this.length = this.endChainage - this.startChainage;
        }
    }

    /**
     * Check if the asset is overdue for inspection.
     */
    public boolean isInspectionOverdue() {
        if (nextInspectionDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(nextInspectionDate);
    }

    /**
     * Check if the asset construction is delayed.
     */
    public boolean isDelayed() {
        if (status != ConstructionStatus.IN_PROGRESS || expectedCompletionDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expectedCompletionDate);
    }

    /**
     * Get remaining construction percentage.
     */
    public double getRemainingPercentage() {
        return 100.0 - (completionPercentage != null ? completionPercentage : 0.0);
    }

    /**
     * Check if the asset is a child asset.
     */
    public boolean isChildAsset() {
        return parentAssetId != null;
    }
}
