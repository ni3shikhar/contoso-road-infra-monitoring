package com.contoso.roadinfra.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entity for tracking user actions across the system.
 * 
 * Audit events are also published to Kafka topic 'audit-events' for
 * consumption by other services requiring audit trail information.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_resource_type", columnList = "resource_type"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    /**
     * Predefined audit actions.
     */
    public static final class Actions {
        // Authentication actions
        public static final String LOGIN = "LOGIN";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String LOGOUT = "LOGOUT";
        public static final String TOKEN_REFRESH = "TOKEN_REFRESH";
        public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
        public static final String PASSWORD_RESET = "PASSWORD_RESET";
        
        // User management actions
        public static final String CREATE_USER = "CREATE_USER";
        public static final String UPDATE_USER = "UPDATE_USER";
        public static final String DELETE_USER = "DELETE_USER";
        public static final String ENABLE_USER = "ENABLE_USER";
        public static final String DISABLE_USER = "DISABLE_USER";
        public static final String LOCK_USER = "LOCK_USER";
        public static final String UNLOCK_USER = "UNLOCK_USER";
        public static final String CHANGE_ROLE = "CHANGE_ROLE";
        
        // Resource actions
        public static final String CREATE_SENSOR = "CREATE_SENSOR";
        public static final String UPDATE_SENSOR = "UPDATE_SENSOR";
        public static final String DELETE_SENSOR = "DELETE_SENSOR";
        public static final String CONFIGURE_SENSOR = "CONFIGURE_SENSOR";
        
        public static final String CREATE_ASSET = "CREATE_ASSET";
        public static final String UPDATE_ASSET = "UPDATE_ASSET";
        public static final String DELETE_ASSET = "DELETE_ASSET";
        
        public static final String ACKNOWLEDGE_ALERT = "ACKNOWLEDGE_ALERT";
        public static final String RESOLVE_ALERT = "RESOLVE_ALERT";
        public static final String ASSIGN_ALERT = "ASSIGN_ALERT";
        
        public static final String EXPORT_DATA = "EXPORT_DATA";
        public static final String VIEW_REPORT = "VIEW_REPORT";

        private Actions() {}
    }

    /**
     * Resource types for audit logging.
     */
    public static final class ResourceTypes {
        public static final String USER = "USER";
        public static final String SENSOR = "SENSOR";
        public static final String ASSET = "ASSET";
        public static final String ALERT = "ALERT";
        public static final String REPORT = "REPORT";
        public static final String INSPECTION = "INSPECTION";
        public static final String SYSTEM = "SYSTEM";

        private ResourceTypes() {}
    }
}
