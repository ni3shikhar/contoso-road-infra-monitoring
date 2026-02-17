package com.contoso.roadinfra.common.security;

import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.constants.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter for downstream microservices.
 * 
 * This filter extracts role and permissions from JWT claims and sets them
 * in Spring SecurityContext as GrantedAuthority objects.
 * 
 * JWT Claims expected:
 * - sub: username
 * - userId: UUID
 * - role: Role enum value (ADMIN/ENGINEER/OPERATOR/VIEWER)
 * - permissions: comma-separated list of Permission enum values
 * - persona: descriptive persona label
 * - department: user's department
 * 
 * Authority format in SecurityContext:
 * - ROLE_ADMIN, ROLE_ENGINEER, etc. for roles
 * - PERM_SENSOR_READ, PERM_ALERT_WRITE, etc. for permissions
 */
@Slf4j
public class JwtClaimsAuthenticationFilter extends OncePerRequestFilter {

    private final String jwtSecret;
    private final List<String> excludedPaths;

    public JwtClaimsAuthenticationFilter(String jwtSecret) {
        this(jwtSecret, List.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/actuator/**", "/swagger-ui/**", "/api-docs/**"));
    }

    public JwtClaimsAuthenticationFilter(String jwtSecret, List<String> excludedPaths) {
        this.jwtSecret = jwtSecret;
        this.excludedPaths = excludedPaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                return path.startsWith(pattern.substring(0, pattern.length() - 3));
            }
            return path.equals(pattern);
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                Claims claims = validateAndExtractClaims(jwt);
                if (claims != null) {
                    JwtUserPrincipal principal = buildPrincipal(claims);
                    List<GrantedAuthority> authorities = buildAuthorities(claims);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authenticated user {} with role {} and {} permissions",
                            principal.getUsername(), principal.getRole(), authorities.size());
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Claims validateAndExtractClaims(String token) {
        try {
            // Use raw bytes to match API gateway's key derivation
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return null;
    }

    private JwtUserPrincipal buildPrincipal(Claims claims) {
        return JwtUserPrincipal.builder()
                .userId(claims.get("userId") != null ? UUID.fromString(claims.get("userId", String.class)) : null)
                .username(claims.getSubject())
                .role(claims.get("role", String.class))
                .persona(claims.get("persona", String.class))
                .department(claims.get("department", String.class))
                .build();
    }

    private List<GrantedAuthority> buildAuthorities(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add role authority
        String role = claims.get("role", String.class);
        if (StringUtils.hasText(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        // Add permission authorities from JWT claims
        String permissionsStr = claims.get("permissions", String.class);
        if (StringUtils.hasText(permissionsStr)) {
            Arrays.stream(permissionsStr.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(perm -> authorities.add(new SimpleGrantedAuthority("PERM_" + perm)));
        }

        return authorities;
    }

    /**
     * Principal object containing JWT claim information.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JwtUserPrincipal {
        private UUID userId;
        private String username;
        private String role;
        private String persona;
        private String department;
    }
}
