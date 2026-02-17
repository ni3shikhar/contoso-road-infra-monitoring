package com.contoso.roadinfra.auth.service;

import com.contoso.roadinfra.auth.entity.AuditLog;
import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.auth.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUDIT_TOPIC = "audit-events";

    /**
     * Log an audit event for the current authenticated user.
     */
    @Async
    public void logEvent(String action, String resourceType, String resourceId, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            logEvent(user.getId(), user.getUsername(), action, resourceType, resourceId, details);
        } else if (auth != null) {
            logEvent(null, auth.getName(), action, resourceType, resourceId, details);
        }
    }

    /**
     * Log an audit event for a specific user.
     */
    @Async
    public void logEvent(UUID userId, String username, String action, String resourceType, 
                         String resourceId, Map<String, Object> details) {
        try {
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .details(detailsJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            // Save to database
            auditLogRepository.save(auditLog);

            // Publish to Kafka
            publishToKafka(auditLog);

            log.debug("Audit event logged: {} by {} on {}:{}",
                    action, username, resourceType, resourceId);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log authentication event (login/logout/failed login).
     */
    public void logAuthEvent(String username, String action, boolean success, String ipAddress) {
        Map<String, Object> details = new HashMap<>();
        details.put("success", success);
        details.put("ipAddress", ipAddress);
        details.put("timestamp", Instant.now().toString());

        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .resourceType(AuditLog.ResourceTypes.USER)
                    .ipAddress(ipAddress)
                    .userAgent(getUserAgent())
                    .details(objectMapper.writeValueAsString(details))
                    .build();

            auditLogRepository.save(auditLog);
            publishToKafka(auditLog);
        } catch (Exception e) {
            log.error("Failed to log auth event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log user management event.
     */
    public void logUserManagementEvent(String adminUsername, String action, UUID targetUserId, 
                                       String targetUsername, Map<String, Object> changes) {
        Map<String, Object> details = new HashMap<>();
        details.put("targetUserId", targetUserId != null ? targetUserId.toString() : null);
        details.put("targetUsername", targetUsername);
        details.put("changes", changes);

        logEventSync(null, adminUsername, action, AuditLog.ResourceTypes.USER, 
                targetUserId != null ? targetUserId.toString() : null, details);
    }

    /**
     * Synchronous event logging (for critical operations).
     */
    public void logEventSync(UUID userId, String username, String action, String resourceType, 
                             String resourceId, Map<String, Object> details) {
        try {
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .details(detailsJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            publishToKafka(auditLog);
        } catch (Exception e) {
            log.error("Failed to log sync audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Query audit logs with filters.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findByFilters(UUID userId, String username, String action,
                                         String resourceType, Instant startDate, Instant endDate,
                                         Pageable pageable) {
        return auditLogRepository.findByFilters(userId, username, action, resourceType, 
                startDate, endDate, pageable);
    }

    /**
     * Get audit logs for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findByUserId(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get all distinct actions.
     */
    @Transactional(readOnly = true)
    public java.util.List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }

    /**
     * Get all distinct resource types.
     */
    @Transactional(readOnly = true)
    public java.util.List<String> getDistinctResourceTypes() {
        return auditLogRepository.findDistinctResourceTypes();
    }

    private void publishToKafka(AuditLog auditLog) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("id", auditLog.getId() != null ? auditLog.getId().toString() : null);
            event.put("userId", auditLog.getUserId() != null ? auditLog.getUserId().toString() : null);
            event.put("username", auditLog.getUsername());
            event.put("action", auditLog.getAction());
            event.put("resourceType", auditLog.getResourceType());
            event.put("resourceId", auditLog.getResourceId());
            event.put("ipAddress", auditLog.getIpAddress());
            event.put("timestamp", auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : Instant.now().toString());

            kafkaTemplate.send(AUDIT_TOPIC, auditLog.getUsername(), event);
        } catch (Exception e) {
            log.warn("Failed to publish audit event to Kafka: {}", e.getMessage());
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP: {}", e.getMessage());
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 255) {
                    userAgent = userAgent.substring(0, 255);
                }
                return userAgent;
            }
        } catch (Exception e) {
            log.debug("Could not get user agent: {}", e.getMessage());
        }
        return null;
    }
}
