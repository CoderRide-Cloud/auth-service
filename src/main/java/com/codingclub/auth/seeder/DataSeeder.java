package com.codingclub.auth.seeder;

import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import com.codingclub.auth.repository.UserRepository;
import com.codingclub.common.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${SUPERADMIN_GITHUB_ID:#{null}}")
    private String superadminGithubId;

    @Value("${SUPERADMIN_USERNAME:#{null}}")
    private String superadminUsername;

    @Value("${SUPERADMIN_EMAIL:#{null}}")
    private String superadminEmail;

    @Value("${SUPERADMIN_AVATAR_URL:#{null}}")
    private String superadminAvatarUrl;

    @Value("${SUPERADMIN_GITHUB_URL:#{null}}")
    private String superadminGithubUrl;

    @Override
    public void run(String... args) {
        log.info("🌱 Checking superadmin data seeder...");

        if (superadminGithubId != null && superadminUsername != null && superadminEmail != null) {
            userRepository.findByGithubId(superadminGithubId).ifPresentOrElse(
                    user -> log.info("✅ Superadmin user {} already exists. Skipping.", superadminUsername),
                    () -> {
                        User user = new User();
                        user.setGithubId(superadminGithubId);
                        user.setUsername(superadminUsername);
                        user.setEmail(superadminEmail);
                        user.setAvatarUrl(superadminAvatarUrl);
                        user.setGithubUrl(superadminGithubUrl);
                        user.setRole(UserRole.ADMIN);
                        user.setIsActive(true);
                        user.setIsLead(true);
                        User savedUser = userRepository.save(user);
                        
                        UserCreatedEvent event = new UserCreatedEvent(
                                savedUser.getId(),
                                savedUser.getGithubId(),
                                savedUser.getUsername(),
                                savedUser.getEmail(),
                                savedUser.getAvatarUrl(),
                                savedUser.getGithubUrl()
                        );
                        kafkaTemplate.send("user-created-topic", String.valueOf(savedUser.getId()), event);
                        
                        log.info("✅ Superadmin user {} seeded successfully.", superadminUsername);
                    }
            );
        } else {
            log.warn("⚠️ Superadmin environment variables not fully set (SUPERADMIN_GITHUB_ID, SUPERADMIN_USERNAME, SUPERADMIN_EMAIL). Skipping superadmin seeding.");
        }
    }
}
