package com.contoso.roadinfra.auth.controller;

import com.contoso.roadinfra.auth.entity.AuditLog;
import com.contoso.roadinfra.auth.service.AuditService;
import com.contoso.roadinfra.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log viewing endpoints (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List audit logs with filters (paginated)")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            Pageable pageable) {
        Page<AuditLog> logs = auditService.findByFilters(userId, username, action, resourceType, startDate, endDate, pageable);
        Page<AuditLogDto> dtos = logs.map(AuditLogDto::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            Pageable pageable) {
        Page<AuditLog> logs = auditService.findByUserId(userId, pageable);
        Page<AuditLogDto> dtos = logs.map(AuditLogDto::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/actions")
    @Operation(summary = "Get all distinct audit actions")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctActions() {
        List<String> actions = auditService.getDistinctActions();
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    @GetMapping("/resource-types")
    @Operation(summary = "Get all distinct resource types")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctResourceTypes() {
        List<String> resourceTypes = auditService.getDistinctResourceTypes();
        return ResponseEntity.ok(ApiResponse.success(resourceTypes));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogDto {
        private UUID id;
        private UUID userId;
        private String username;
        private String action;
        private String resourceType;
        private String resourceId;
        private String details;
        private String ipAddress;
        private String userAgent;
        private Instant timestamp;

        public static AuditLogDto fromEntity(AuditLog log) {
            return AuditLogDto.builder()
                    .id(log.getId())
                    .userId(log.getUserId())
                    .username(log.getUsername())
                    .action(log.getAction())
                    .resourceType(log.getResourceType())
                    .resourceId(log.getResourceId())
                    .details(log.getDetails())
                    .ipAddress(log.getIpAddress())
                    .userAgent(log.getUserAgent())
                    .timestamp(log.getTimestamp())
                    .build();
        }
    }
}
