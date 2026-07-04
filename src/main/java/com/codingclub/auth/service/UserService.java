package com.codingclub.auth.service;

import com.codingclub.auth.client.RoleServiceClient;
import com.codingclub.auth.dto.CustomRoleDto;
import com.codingclub.auth.dto.GithubAuthRequest;
import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import com.codingclub.auth.repository.UserRepository;
import com.codingclub.common.event.UserApprovedEvent;
import com.codingclub.common.event.UserCreatedEvent;
import com.codingclub.common.event.UserRejectedEvent;
import com.codingclub.common.exception.ResourceNotFoundException;
import com.codingclub.common.security.Permission;
import com.codingclub.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.codingclub.common.dto.UserDto;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RoleServiceClient roleServiceClient;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String USER_CREATED_TOPIC = "user-created-topic";
    private static final String USER_APPROVED_TOPIC = "user-approved-topic";
    private static final String USER_REJECTED_TOPIC = "user-rejected-topic";

    public Optional<User> findByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public String generateTokenForUser(User user) {
        // resolveCustomRole is now cached via Redis — no Feign call on repeated logins
        CustomRoleDto customRole = resolveCustomRole(user.getCustomRoleId());
        Set<Permission> permissions = customRole != null && customRole.getPermissions() != null
                ? customRole.getPermissions()
                : Collections.emptySet();
        Integer position = customRole != null ? customRole.getPosition() : 0;

        return jwtUtil.generateToken(
                user.getId(),
                user.getRole().name(),
                user.getCustomRoleId(),
                position,
                user.getIsLead(),
                user.getIsActive(),
                permissions
        );
    }

    @Transactional
    public User getOrCreateUser(GithubAuthRequest request) {
        Optional<User> existingUser = userRepository.findByGithubId(request.getGithubId());

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = new User();
        newUser.setGithubId(request.getGithubId());
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setAvatarUrl(request.getAvatarUrl());
        newUser.setGithubUrl(request.getGithubUrl());
        newUser.setIsActive(false);
        newUser.setRole(UserRole.PENDING);

        User savedUser = userRepository.save(newUser);

        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getGithubId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getAvatarUrl(),
                savedUser.getGithubUrl()
        );
        kafkaTemplate.send(USER_CREATED_TOPIC, String.valueOf(savedUser.getId()), event);

        return savedUser;
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getPendingMembers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(UserRole.PENDING);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByCustomRoleId(Long customRoleId) {
        return userRepository.findByCustomRoleId(customRoleId);
    }

    public List<User> getLeads() {
        return userRepository.findByIsLeadTrueAndIsActiveTrueAndRole(UserRole.MEMBER);
    }

    @Transactional
    @CacheEvict(value = "customRoles", key = "#userId")
    public User approveMember(Long userId, UserRole role, Long customRoleId, String suspensionReason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserRole previousRole = user.getRole();
        user.setRole(role);
        user.setCustomRoleId(customRoleId);
        user.setSuspensionReason(suspensionReason);
        user.setIsActive(role == UserRole.MEMBER || role == UserRole.ADMIN);

        User saved = userRepository.save(user);
        publishRoleChangeEvents(saved, previousRole, suspensionReason);
        return saved;
    }

    @Transactional
    public User updateUserRole(User user, UserRole newRole, Long customRoleId, String reason) {
        UserRole previousRole = user.getRole();
        user.setRole(newRole);
        if (customRoleId != null || newRole == UserRole.SUSPENDED || newRole == UserRole.PENDING) {
            user.setCustomRoleId(customRoleId);
        }
        if (reason != null) {
            user.setSuspensionReason(reason);
        }
        user.setIsActive(newRole == UserRole.MEMBER || newRole == UserRole.ADMIN);

        User saved = userRepository.save(user);
        publishRoleChangeEvents(saved, previousRole, reason);
        return saved;
    }

    private void publishRoleChangeEvents(User user, UserRole previousRole, String reason) {
        if (previousRole == UserRole.PENDING && (user.getRole() == UserRole.MEMBER || user.getRole() == UserRole.ADMIN)) {
            kafkaTemplate.send(USER_APPROVED_TOPIC, String.valueOf(user.getId()),
                    new UserApprovedEvent(user.getId(), user.getUsername(), user.getEmail()));
        } else if (user.getRole() == UserRole.SUSPENDED) {
            kafkaTemplate.send(USER_REJECTED_TOPIC, String.valueOf(user.getId()),
                    new UserRejectedEvent(user.getId(), user.getUsername(), user.getEmail(), reason));
        }
    }

    /**
     * OPTIMIZED: Role lookup is cached in Redis for 5 minutes (configured in application.yml).
     * On first login this calls role-service via Feign. Subsequent logins for the same
     * custom role ID (e.g. dozens of users with same role) are served from Redis cache.
     */
    @Cacheable(value = "customRoles", key = "#customRoleId", unless = "#result == null")
    public CustomRoleDto resolveCustomRole(Long customRoleId) {
        if (customRoleId == null) {
            return null;
        }
        try {
            return roleServiceClient.getRoleById(customRoleId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<UserDto> getUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .githubId(user.getGithubId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .githubUrl(user.getGithubUrl())
                .role(user.getRole().name())
                .customRoleId(user.getCustomRoleId())
                .suspensionReason(user.getSuspensionReason())
                .isActive(user.getIsActive())
                .isLead(user.getIsLead())
                .build();
    }
}
