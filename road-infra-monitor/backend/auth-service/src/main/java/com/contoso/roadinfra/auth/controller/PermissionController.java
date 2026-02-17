package com.contoso.roadinfra.auth.controller;

import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.constants.Role;
import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.common.security.RolePermissionMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Permissions", description = "Permission and role information endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    @GetMapping("/permissions")
    @Operation(summary = "List all available permissions")
    public ResponseEntity<ApiResponse<List<PermissionInfo>>> getAllPermissions() {
        List<PermissionInfo> permissions = Arrays.stream(Permission.values())
                .map(p -> PermissionInfo.builder()
                        .name(p.name())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/permissions/role/{role}")
    @Operation(summary = "Get permissions for a specific role")
    public ResponseEntity<ApiResponse<RolePermissions>> getPermissionsForRole(@PathVariable Role role) {
        Set<Permission> permissions = RolePermissionMapping.getPermissions(role);
        
        RolePermissions rolePermissions = RolePermissions.builder()
                .role(role.name())
                .roleDisplayName(role.getDisplayName())
                .permissions(permissions.stream()
                        .map(p -> PermissionInfo.builder()
                                .name(p.name())
                                .description(p.getDescription())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(rolePermissions));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all roles with descriptions and persona mappings")
    public ResponseEntity<ApiResponse<List<RoleInfo>>> getAllRoles() {
        List<RoleInfo> roles = List.of(
                RoleInfo.builder()
                        .name(Role.ADMIN.name())
                        .displayName(Role.ADMIN.getDisplayName())
                        .description("Full platform configuration, user management, and system maintenance")
                        .personas(List.of("System Administrator"))
                        .permissionCount(RolePermissionMapping.getPermissions(Role.ADMIN).size())
                        .build(),
                RoleInfo.builder()
                        .name(Role.ENGINEER.name())
                        .displayName(Role.ENGINEER.getDisplayName())
                        .description("Technical access for sensor configuration, threshold tuning, and data analysis")
                        .personas(List.of(
                                "Structural/Civil Engineer",
                                "IoT/Instrumentation Technician",
                                "Data Analyst/Asset Planner"))
                        .permissionCount(RolePermissionMapping.getPermissions(Role.ENGINEER).size())
                        .build(),
                RoleInfo.builder()
                        .name(Role.OPERATOR.name())
                        .displayName(Role.OPERATOR.getDisplayName())
                        .description("Operational access for construction tracking, maintenance, and alert handling")
                        .personas(List.of(
                                "Site/Project Manager",
                                "Maintenance/Operations Manager",
                                "Safety Officer"))
                        .permissionCount(RolePermissionMapping.getPermissions(Role.OPERATOR).size())
                        .build(),
                RoleInfo.builder()
                        .name(Role.VIEWER.name())
                        .displayName(Role.VIEWER.getDisplayName())
                        .description("Read-only access for dashboards, KPIs, and compliance reporting")
                        .personas(List.of(
                                "Executive/Project Sponsor",
                                "Regulatory Inspector"))
                        .permissionCount(RolePermissionMapping.getPermissions(Role.VIEWER).size())
                        .build()
        );
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionInfo {
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissions {
        private String role;
        private String roleDisplayName;
        private List<PermissionInfo> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private String name;
        private String displayName;
        private String description;
        private List<String> personas;
        private int permissionCount;
    }
}
