package com.contoso.roadinfra.auth.config;

import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.auth.repository.UserRepository;
import com.contoso.roadinfra.common.constants.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data loader for seeding default users.
 * Only runs in development/local profile.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "local", "default", "docker"})
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping seed data");
            return;
        }

        log.info("Seeding default users...");
        
        List<User> seedUsers = List.of(
                createUser("admin", "Admin@123", "admin@contoso.com", "System", "Administrator",
                        Role.ADMIN, "System Administrator", "IT Department"),

                createUser("j.engineer", "Eng@123", "j.engineer@contoso.com", "James", "Engineer",
                        Role.ENGINEER, "Structural Engineer", "Engineering"),

                createUser("s.iottech", "IoT@123", "s.iottech@contoso.com", "Sarah", "IoTTech",
                        Role.ENGINEER, "IoT Technician", "Engineering"),

                createUser("d.analyst", "Data@123", "d.analyst@contoso.com", "David", "Analyst",
                        Role.ENGINEER, "Data Analyst", "Planning"),

                createUser("m.projmgr", "Proj@123", "m.projmgr@contoso.com", "Maria", "ProjectManager",
                        Role.OPERATOR, "Site Project Manager", "Construction"),

                createUser("k.maintenance", "Maint@123", "k.maintenance@contoso.com", "Kevin", "Maintenance",
                        Role.OPERATOR, "Maintenance Manager", "Operations"),

                createUser("r.safety", "Safe@123", "r.safety@contoso.com", "Rachel", "Safety",
                        Role.OPERATOR, "Safety Officer", "Safety"),

                createUser("e.director", "Exec@123", "e.director@contoso.com", "Emily", "Director",
                        Role.VIEWER, "Executive Director", "Management"),

                createUser("i.inspector", "Insp@123", "i.inspector@contoso.com", "Ivan", "Inspector",
                        Role.VIEWER, "Regulatory Inspector", "Compliance")
        );

        userRepository.saveAll(seedUsers);
        log.info("Seeded {} default users", seedUsers.size());
    }

    private User createUser(String username, String password, String email,
                            String firstName, String lastName, Role role,
                            String persona, String department) {
        return User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .persona(persona)
                .department(department)
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mustChangePassword(false)
                .deleted(false)
                .createdBy("system")
                .build();
    }
}
