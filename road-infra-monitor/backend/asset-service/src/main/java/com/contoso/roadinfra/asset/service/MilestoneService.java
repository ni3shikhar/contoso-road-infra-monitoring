package com.contoso.roadinfra.asset.service;

import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import com.contoso.roadinfra.asset.dto.*;
import com.contoso.roadinfra.asset.entity.Asset;
import com.contoso.roadinfra.asset.entity.ConstructionMilestone;
import com.contoso.roadinfra.asset.event.AssetEventPublisher;
import com.contoso.roadinfra.asset.mapper.MilestoneMapper;
import com.contoso.roadinfra.asset.repository.AssetRepository;
import com.contoso.roadinfra.asset.repository.MilestoneRepository;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing construction milestones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final AssetRepository assetRepository;
    private final MilestoneMapper milestoneMapper;
    private final AssetEventPublisher eventPublisher;
    private final AssetService assetService;

    /**
     * Create a new milestone for an asset.
     */
    @CacheEvict(value = {"milestones", "delayedMilestones", "gantt"}, allEntries = true)
    public MilestoneResponse createMilestone(UUID assetId, MilestoneCreateRequest request, String username) {
        log.info("Creating milestone '{}' for asset {} by {}", request.getMilestoneName(), assetId, username);

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));

        ConstructionMilestone milestone = milestoneMapper.toEntity(request);
        milestone.setAsset(asset);
        milestone.setStatus(MilestoneStatus.PENDING);
        milestone.setCreatedBy(username);
        milestone.setUpdatedBy(username);

        ConstructionMilestone saved = milestoneRepository.save(milestone);
        log.info("Milestone {} created for asset {}", saved.getId(), assetId);

        eventPublisher.publishMilestoneCreated(saved);

        return milestoneMapper.toResponse(saved);
    }

    /**
     * Update a milestone.
     */
    @CacheEvict(value = {"milestones", "delayedMilestones", "gantt"}, allEntries = true)
    public MilestoneResponse updateMilestone(UUID milestoneId, MilestoneUpdateRequest request, String username) {
        log.info("Updating milestone {} by {}", milestoneId, username);

        ConstructionMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        milestoneMapper.updateEntity(request, milestone);
        milestone.setUpdatedBy(username);

        ConstructionMilestone updated = milestoneRepository.save(milestone);
        log.info("Milestone {} updated", milestoneId);

        eventPublisher.publishMilestoneUpdated(updated);

        return milestoneMapper.toResponse(updated);
    }

    /**
     * Complete a milestone.
     */
    @CacheEvict(value = {"milestones", "delayedMilestones", "gantt", "assets", "corridorSummary"}, allEntries = true)
    public MilestoneResponse completeMilestone(UUID milestoneId, MilestoneCompleteRequest request, String username) {
        log.info("Completing milestone {} by {}", milestoneId, username);

        ConstructionMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            throw new IllegalStateException("Milestone is already completed");
        }

        milestone.complete(request.getNotes());
        milestone.setUpdatedBy(username);

        ConstructionMilestone completed = milestoneRepository.save(milestone);
        log.info("Milestone {} completed{}", milestoneId, 
                milestone.isDelayed() ? " (delayed by " + milestone.calculateDelayDays() + " days)" : " on time");

        // Recalculate asset completion percentage
        Double newPercentage = assetService.recalculateCompletionFromMilestones(milestone.getAsset().getId());
        log.info("Asset {} completion updated to {}%", milestone.getAsset().getId(), newPercentage);

        eventPublisher.publishMilestoneCompleted(completed);

        return milestoneMapper.toResponse(completed);
    }

    /**
     * Get milestones for an asset.
     */
    @Cacheable(value = "milestones", key = "#assetId + '-' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<MilestoneResponse> getMilestonesForAsset(UUID assetId, Pageable pageable) {
        log.debug("Fetching milestones for asset {}", assetId);

        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException("Asset", assetId);
        }

        return milestoneRepository.findByAssetIdOrderBySequenceOrder(assetId, pageable)
                .map(milestoneMapper::toResponse);
    }

    /**
     * Get a specific milestone by ID.
     */
    @Transactional(readOnly = true)
    public MilestoneResponse getMilestoneById(UUID milestoneId) {
        log.debug("Fetching milestone {}", milestoneId);
        ConstructionMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));
        return milestoneMapper.toResponse(milestone);
    }

    /**
     * Get all delayed milestones.
     */
    @Cacheable(value = "delayedMilestones")
    @Transactional(readOnly = true)
    public List<DelayedMilestoneResponse> getDelayedMilestones() {
        log.debug("Fetching delayed milestones");

        List<ConstructionMilestone> delayed = milestoneRepository.findDelayedMilestones(LocalDate.now());

        return delayed.stream()
                .map(m -> DelayedMilestoneResponse.builder()
                        .milestoneId(m.getId())
                        .milestoneName(m.getName())
                        .assetId(m.getAsset().getId())
                        .assetCode(m.getAsset().getAssetCode())
                        .assetName(m.getAsset().getName())
                        .plannedDate(m.getPlannedDate())
                        .daysOverdue(m.calculateDelayDays())
                        .status(m.getStatus())
                        .weight(m.getWeight())
                        .build())
                .sorted((a, b) -> Integer.compare(b.getDaysOverdue(), a.getDaysOverdue()))
                .collect(Collectors.toList());
    }

    /**
     * Get Gantt chart data for an asset.
     */
    @Cacheable(value = "gantt", key = "#assetId")
    @Transactional(readOnly = true)
    public GanttChartResponse getGanttChart(UUID assetId) {
        log.debug("Generating Gantt chart for asset {}", assetId);

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));

        List<ConstructionMilestone> milestones = milestoneRepository.findByAssetIdOrderBySequenceOrder(assetId);

        LocalDate projectStart = asset.getConstructionStartDate();
        LocalDate projectEnd = asset.getExpectedCompletionDate();

        // Calculate from milestones if not set
        if (projectStart == null && !milestones.isEmpty()) {
            projectStart = milestones.stream()
                    .map(ConstructionMilestone::getPlannedDate)
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        }

        if (projectEnd == null && !milestones.isEmpty()) {
            projectEnd = milestones.stream()
                    .map(ConstructionMilestone::getPlannedDate)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now().plusMonths(6));
        }

        List<GanttChartResponse.GanttTask> tasks = milestones.stream()
                .map(m -> GanttChartResponse.GanttTask.builder()
                        .id(m.getId().toString())
                        .name(m.getName())
                        .plannedStart(m.getPlannedDate().minusDays(7)) // Assume 1 week duration
                        .plannedEnd(m.getPlannedDate())
                        .actualStart(m.getActualCompletionDate() != null ?
                                m.getActualCompletionDate().minusDays(7) : null)
                        .actualEnd(m.getActualCompletionDate())
                        .status(m.getStatus())
                        .progress(m.getStatus() == MilestoneStatus.COMPLETED ? 100.0 :
                                m.getStatus() == MilestoneStatus.IN_PROGRESS ? 50.0 : 0.0)
                        .weight(m.getWeight())
                        .delayDays(m.calculateDelayDays())
                        .build())
                .collect(Collectors.toList());

        // Calculate critical path (simplified - milestones with weight >= 0.2)
        List<String> criticalPath = tasks.stream()
                .filter(t -> t.getWeight() != null && t.getWeight() >= 0.2)
                .map(GanttChartResponse.GanttTask::getId)
                .collect(Collectors.toList());

        return GanttChartResponse.builder()
                .assetId(assetId)
                .assetName(asset.getName())
                .projectStartDate(projectStart)
                .projectEndDate(projectEnd)
                .tasks(tasks)
                .totalMilestones(milestones.size())
                .completedMilestones((int) milestones.stream()
                        .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count())
                .delayedMilestones((int) milestones.stream()
                        .filter(m -> m.isDelayed() || m.getStatus() == MilestoneStatus.DELAYED).count())
                .overallProgress(asset.getCompletionPercentage())
                .criticalPath(criticalPath)
                .build();
    }

    /**
     * Start work on a milestone.
     */
    @CacheEvict(value = {"milestones", "gantt"}, allEntries = true)
    public MilestoneResponse startMilestone(UUID milestoneId, String username) {
        log.info("Starting milestone {} by {}", milestoneId, username);

        ConstructionMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        if (milestone.getStatus() != MilestoneStatus.PENDING) {
            throw new IllegalStateException("Only PENDING milestones can be started");
        }

        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setUpdatedBy(username);

        ConstructionMilestone started = milestoneRepository.save(milestone);
        log.info("Milestone {} started", milestoneId);

        eventPublisher.publishMilestoneUpdated(started);

        return milestoneMapper.toResponse(started);
    }

    /**
     * Mark a milestone as delayed.
     */
    @CacheEvict(value = {"milestones", "delayedMilestones", "gantt"}, allEntries = true)
    public MilestoneResponse markDelayed(UUID milestoneId, String reason, String username) {
        log.info("Marking milestone {} as delayed by {}: {}", milestoneId, username, reason);

        ConstructionMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        milestone.setStatus(MilestoneStatus.DELAYED);
        String notes = milestone.getNotes() != null ?
                milestone.getNotes() + "\nDelay reason: " + reason :
                "Delay reason: " + reason;
        milestone.setNotes(notes);
        milestone.setUpdatedBy(username);

        ConstructionMilestone delayed = milestoneRepository.save(milestone);
        log.warn("Milestone {} marked as delayed", milestoneId);

        eventPublisher.publishMilestoneUpdated(delayed);

        return milestoneMapper.toResponse(delayed);
    }

    /**
     * Delete a milestone.
     */
    @CacheEvict(value = {"milestones", "delayedMilestones", "gantt", "assets", "corridorSummary"}, allEntries = true)
    public void deleteMilestone(UUID milestoneId, String username) {
        log.info("Deleting milestone {} by {}", milestoneId, username);

        ConstructionMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        UUID assetId = milestone.getAsset().getId();
        milestoneRepository.delete(milestone);

        // Recalculate asset completion
        assetService.recalculateCompletionFromMilestones(assetId);

        log.info("Milestone {} deleted", milestoneId);
    }

    // Inner class for delayed milestone response
    @lombok.Builder
    @lombok.Data
    public static class DelayedMilestoneResponse {
        private UUID milestoneId;
        private String milestoneName;
        private UUID assetId;
        private String assetCode;
        private String assetName;
        private LocalDate plannedDate;
        private int daysOverdue;
        private MilestoneStatus status;
        private Double weight;
    }
}
