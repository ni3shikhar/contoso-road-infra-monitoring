package com.contoso.roadinfra.auth.entity;

import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.constants.Role;
import com.contoso.roadinfra.common.security.RolePermissionMapping;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User entity for authentication and authorization.
 * 
 * Supports 4 roles mapped to 8+ real-world personas:
 * - ADMIN: System Administrator
 * - ENGINEER: Structural/Civil Engineer, IoT/Instrumentation Tech, Data Analyst
 * - OPERATOR: Site/Project Manager, Maintenance/Ops Manager, Safety Officer
 * - VIEWER: Executive/Project Sponsor, Regulatory Inspector
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_department", columnList = "department")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 50)
    private String persona;

    @Column(length = 50)
    private String department;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add role authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        
        // Add permission authorities from RolePermissionMapping
        RolePermissionMapping.getPermissions(role).forEach(permission ->
            authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name()))
        );
        
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (!accountLocked) return true;
        if (lockedUntil == null) return false;
        return Instant.now().isAfter(lockedUntil);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !deleted;
    }

    /**
     * Get the full name of the user.
     */
    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    /**
     * Get permissions for this user based on role.
     */
    public java.util.Set<Permission> getPermissions() {
        return RolePermissionMapping.getPermissions(role);
    }
}
