package com.contoso.roadinfra.asset.entity;

import com.contoso.roadinfra.asset.constants.InspectionType;
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
 * Represents an inspection record for an infrastructure asset.
 */
@Entity
@Table(name = "asset_inspections", indexes = {
        @Index(name = "idx_inspection_asset", columnList = "asset_id"),
        @Index(name = "idx_inspection_date", columnList = "inspection_date"),
        @Index(name = "idx_inspection_type", columnList = "inspection_type"),
        @Index(name = "idx_inspection_rating", columnList = "overall_condition_rating")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "inspector_name", nullable = false, length = 200)
    private String inspectorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_type", nullable = false, length = 30)
    private InspectionType inspectionType;

    /**
     * Overall condition rating on a 1-5 scale:
     * 1 = Critical - Immediate action required
     * 2 = Poor - Significant deficiencies
     * 3 = Fair - Some deficiencies
     * 4 = Good - Minor deficiencies
     * 5 = Excellent - No deficiencies
     */
    @Column(name = "overall_condition_rating", nullable = false)
    private Integer overallConditionRating;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "next_inspection_recommended_date")
    private LocalDate nextInspectionRecommendedDate;

    /**
     * URLs to inspection photos stored in object storage.
     */
    @ElementCollection
    @CollectionTable(name = "inspection_photos", joinColumns = @JoinColumn(name = "inspection_id"))
    @Column(name = "photo_url", length = 500)
    @Builder.Default
    private List<String> photos = new ArrayList<>();

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
     * Get the condition description based on rating.
     */
    public String getConditionDescription() {
        return switch (overallConditionRating) {
            case 1 -> "Critical - Immediate action required";
            case 2 -> "Poor - Significant deficiencies";
            case 3 -> "Fair - Some deficiencies";
            case 4 -> "Good - Minor deficiencies";
            case 5 -> "Excellent - No deficiencies";
            default -> "Unknown";
        };
    }

    /**
     * Check if the inspection indicates critical condition.
     */
    public boolean isCritical() {
        return overallConditionRating != null && overallConditionRating <= 2;
    }
}
