package com.contoso.roadinfra.sensor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> {
            // Parse token to determine user type for testing
            if (token.contains("admin")) {
                return createJwt("admin-user", List.of("ADMIN"));
            } else if (token.contains("operator")) {
                return createJwt("operator-user", List.of("OPERATOR"));
            } else if (token.contains("viewer")) {
                return createJwt("viewer-user", List.of("VIEWER"));
            } else if (token.contains("technician")) {
                return createJwt("technician-user", List.of("TECHNICIAN"));
            }
            return createJwt("test-user", List.of("VIEWER"));
        };
    }

    private Jwt createJwt(String subject, List<String> roles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS512")
                .header("typ", "JWT")
                .subject(subject)
                .claim("roles", roles)
                .claim("permissions", getPermissionsForRoles(roles))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
    }

    private List<String> getPermissionsForRoles(List<String> roles) {
        if (roles.contains("ADMIN")) {
            return List.of(
                    "SENSOR_READ", "SENSOR_WRITE", "SENSOR_DELETE", "SENSOR_CONFIGURE",
                    "ALERT_READ", "ALERT_WRITE", "ALERT_ACKNOWLEDGE",
                    "ANALYTICS_READ", "ANALYTICS_EXPORT",
                    "USER_READ", "USER_WRITE", "USER_DELETE",
                    "SYSTEM_CONFIGURE"
            );
        } else if (roles.contains("OPERATOR")) {
            return List.of(
                    "SENSOR_READ", "SENSOR_WRITE", "SENSOR_CONFIGURE",
                    "ALERT_READ", "ALERT_WRITE", "ALERT_ACKNOWLEDGE",
                    "ANALYTICS_READ"
            );
        } else if (roles.contains("TECHNICIAN")) {
            return List.of(
                    "SENSOR_READ", "SENSOR_CONFIGURE",
                    "ALERT_READ", "ALERT_ACKNOWLEDGE"
            );
        } else if (roles.contains("VIEWER")) {
            return List.of(
                    "SENSOR_READ",
                    "ALERT_READ",
                    "ANALYTICS_READ"
            );
        }
        return List.of();
    }
}
