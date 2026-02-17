package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gantt chart data for asset milestones")
public class GanttChartResponse {

    @Schema(description = "Asset ID")
    private UUID assetId;

    @Schema(description = "Asset name")
    private String assetName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Project start date")
    private LocalDate projectStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Project end date")
    private LocalDate projectEndDate;

    @Schema(description = "Milestone tasks")
    private List<GanttTask> tasks;

    @Schema(description = "Total number of milestones")
    private Integer totalMilestones;

    @Schema(description = "Number of completed milestones")
    private Integer completedMilestones;

    @Schema(description = "Number of delayed milestones")
    private Integer delayedMilestones;

    @Schema(description = "Overall progress percentage")
    private Double overallProgress;

    @Schema(description = "Critical path task IDs")
    private List<String> criticalPath;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Single Gantt task/milestone")
    public static class GanttTask {

        @Schema(description = "Task ID")
        private String id;

        @Schema(description = "Task name")
        private String name;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Planned start date")
        private LocalDate plannedStart;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Planned end date")
        private LocalDate plannedEnd;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Actual start date")
        private LocalDate actualStart;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Actual end date")
        private LocalDate actualEnd;

        @Schema(description = "Task status")
        private MilestoneStatus status;

        @Schema(description = "Progress percentage (0-100)")
        private Double progress;

        @Schema(description = "Weight towards completion")
        private Double weight;

        @Schema(description = "Number of days delayed")
        private Integer delayDays;
    }
}
