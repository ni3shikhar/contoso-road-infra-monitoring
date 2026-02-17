package com.contoso.roadinfra.auth.dto;

import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.common.constants.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String persona;
        private String department;
        private List<String> permissions;
        private Instant lastLoginAt;
        private Boolean mustChangePassword;
    }

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, User user) {
        Set<Permission> permissions = user.getPermissions();
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .persona(user.getPersona())
                        .department(user.getDepartment())
                        .permissions(permissions.stream()
                                .map(Enum::name)
                                .sorted()
                                .collect(Collectors.toList()))
                        .lastLoginAt(user.getLastLoginAt())
                        .mustChangePassword(user.getMustChangePassword())
                        .build())
                .build();
    }
}
