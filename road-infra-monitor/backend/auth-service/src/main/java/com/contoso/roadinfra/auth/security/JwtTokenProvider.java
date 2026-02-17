package com.contoso.roadinfra.auth.security;

import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.security.RolePermissionMapping;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * 
 * JWT access token includes these claims:
 * - sub: username
 * - userId: UUID
 * - role: Role enum value (ADMIN/ENGINEER/OPERATOR/VIEWER)
 * - persona: descriptive persona label
 * - permissions: comma-separated list of Permission enum values
 * - department: user's department
 * - iat, exp: issued at and expiry timestamps
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:900000}")  // Default 15 minutes
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")  // Default 7 days
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        // Use raw bytes to match API gateway's key derivation
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT access token with full claims.
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        // Get permissions for user's role
        Set<Permission> permissions = RolePermissionMapping.getPermissions(user.getRole());
        String permissionsStr = permissions.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("persona", user.getPersona())
                .claim("permissions", permissionsStr)
                .claim("department", user.getDepartment())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate refresh token with user ID claim.
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())  // Unique jti to prevent duplicate tokens
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Get username from access token (stored in subject).
     */
    public String getUsernameFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    /**
     * Get user ID from access token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        String userIdStr = claims.get("userId", String.class);
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Get role from access token.
     */
    public String getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extract all claims from token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get access token expiration time in milliseconds.
     */
    public long getAccessTokenExpiration() {
        return jwtExpiration;
    }

    /**
     * Get refresh token expiration time in milliseconds.
     */
    public long getRefreshTokenExpiration() {
        return refreshExpiration;
    }

    /**
     * Get the JWT secret (for sharing with other services).
     */
    public String getJwtSecret() {
        return jwtSecret;
    }
}
