package com.contoso.roadinfra.asset.entity;

import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Represents a construction milestone for an infrastructure asset.
 */
@Entity
@Table(name = "construction_milestones", indexes = {
        @Index(name = "idx_milestone_asset", columnList = "asset_id"),
        @Index(name = "idx_milestone_status", columnList = "status"),
        @Index(name = "idx_milestone_planned_date", columnList = "planned_date"),
        @Index(name = "idx_milestone_sequence", columnList = "asset_id, sequence_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstructionMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MilestoneStatus status = MilestoneStatus.PENDING;

    /**
     * Weight of milestone towards overall completion (0.0 - 1.0).
     */
    @Column(name = "weight")
    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Order in which milestones should be completed.
     */
    @Column(name = "sequence_order")
    @Builder.Default
    private Integer sequenceOrder = 0;

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
     * Calculate delay days based on planned and actual dates.
     * Returns the number of days delayed (positive = late, negative = early).
     */
    public int calculateDelayDays() {
        if (plannedDate == null) {
            return 0;
        }

        if (status == MilestoneStatus.COMPLETED && actualCompletionDate != null) {
            return (int) ChronoUnit.DAYS.between(plannedDate, actualCompletionDate);
        } else if (status == MilestoneStatus.PENDING || status == MilestoneStatus.IN_PROGRESS) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(plannedDate)) {
                return (int) ChronoUnit.DAYS.between(plannedDate, today);
            }
        }
        return 0;
    }

    /**
     * Check if the milestone is currently delayed.
     */
    public boolean isDelayed() {
        if (status == MilestoneStatus.COMPLETED || status == MilestoneStatus.CANCELLED) {
            return actualCompletionDate != null && actualCompletionDate.isAfter(plannedDate);
        }
        return plannedDate != null && LocalDate.now().isAfter(plannedDate);
    }

    /**
     * Check if the milestone can be marked as complete.
     */
    public boolean canComplete() {
        return status == MilestoneStatus.PENDING || 
               status == MilestoneStatus.IN_PROGRESS || 
               status == MilestoneStatus.DELAYED;
    }

    /**
     * Mark milestone as complete with optional notes.
     */
    public void complete(String completionNotes) {
        this.actualCompletionDate = LocalDate.now();
        this.status = MilestoneStatus.COMPLETED;
        if (completionNotes != null && !completionNotes.isBlank()) {
            this.notes = this.notes != null ? this.notes + "\n" + completionNotes : completionNotes;
        }
    }
}
