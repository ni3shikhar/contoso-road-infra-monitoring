package com.contoso.roadinfra.common.security;

import com.contoso.roadinfra.common.constants.Permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for method-level security based on permissions.
 * 
 * This annotation is processed by the PermissionAspect to enforce
 * permission-based access control on controller methods or service methods.
 * 
 * Usage:
 * <pre>
 * {@code
 * @RequiresPermission(Permission.SENSOR_READ)
 * public List<SensorDTO> getAllSensors() { ... }
 * 
 * @RequiresPermission({Permission.ALERT_ACKNOWLEDGE, Permission.ALERT_RESOLVE})
 * public void handleAlert(Long alertId) { ... }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    
    /**
     * The permission(s) required to execute the annotated method.
     * If multiple permissions are specified, the user must have at least
     * one of them (OR logic) unless {@link #requireAll()} is set to true.
     *
     * @return array of required permissions
     */
    Permission[] value();

    /**
     * If true, the user must have ALL specified permissions (AND logic).
     * If false (default), the user only needs ONE of the permissions (OR logic).
     *
     * @return whether all permissions are required
     */
    boolean requireAll() default false;

    /**
     * Optional message to return when permission is denied.
     *
     * @return the denial message
     */
    String message() default "Access denied: insufficient permissions";
}
