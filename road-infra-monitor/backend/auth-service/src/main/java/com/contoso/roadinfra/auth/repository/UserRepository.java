package com.contoso.roadinfra.auth.repository;

import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.common.constants.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByEmailAndDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.deleted = false")
    Page<User> findAllActive(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deleted = false " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:department IS NULL OR u.department = :department) " +
           "AND (:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> findByFilters(
            @Param("role") Role role,
            @Param("department") String department,
            @Param("enabled") Boolean enabled,
            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.role = :role")
    List<User> findByRole(@Param("role") Role role);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.department = :department")
    List<User> findByDepartment(@Param("department") String department);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLogin, u.lastLoginIp = :ipAddress WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("lastLogin") Instant lastLogin, @Param("ipAddress") String ipAddress);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedAttempts(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLocked = false, u.lockedUntil = null WHERE u.id = :userId")
    void resetFailedAttempts(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true, u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") UUID userId, @Param("lockedUntil") Instant lockedUntil);

    @Modifying
    @Query("UPDATE User u SET u.accountLocked = false, u.lockedUntil = null, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockAccount(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    void setEnabled(@Param("userId") UUID userId, @Param("enabled") boolean enabled);

    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :userId")
    void updateRole(@Param("userId") UUID userId, @Param("role") Role role);

    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.passwordChangedAt = :changedAt, u.mustChangePassword = false WHERE u.id = :userId")
    void updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash, @Param("changedAt") Instant changedAt);

    @Modifying
    @Query("UPDATE User u SET u.mustChangePassword = true WHERE u.id = :userId")
    void forcePasswordReset(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.deleted = true WHERE u.id = :userId")
    void softDelete(@Param("userId") UUID userId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false AND u.role = :role")
    long countByRole(@Param("role") Role role);
}
