package com.contoso.roadinfra.auth.dto;

import com.contoso.roadinfra.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String persona;
    private String department;
    private String phoneNumber;
    private Boolean enabled;
    private Boolean accountLocked;
    private Integer failedLoginAttempts;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private Boolean mustChangePassword;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private List<String> permissions;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .persona(user.getPersona())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .mustChangePassword(user.getMustChangePassword())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .permissions(user.getPermissions().stream()
                        .map(Enum::name)
                        .sorted()
                        .collect(Collectors.toList()))
                .build();
    }
}
