package com.codingclub.auth.controller;

import com.codingclub.auth.client.RoleServiceClient;
import com.codingclub.auth.dto.CustomRoleDto;
import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import com.codingclub.auth.service.UserService;
import com.codingclub.common.exception.ResourceNotFoundException;
import com.codingclub.common.security.AuthUserContext;
import com.codingclub.common.security.AuthorizationService;
import com.codingclub.common.security.Permission;
import com.codingclub.common.web.AuthContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.codingclub.common.dto.UserDto;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthContextResolver authContextResolver;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RoleServiceClient roleServiceClient;

    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingMembers(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.VIEW_DASHBOARD);
        return ResponseEntity.ok(userService.getPendingMembers());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.VIEW_DASHBOARD);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<User>> getRoleUsers(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable Long roleId) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_ROLES);
        return ResponseEntity.ok(userService.getUsersByCustomRoleId(roleId));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<UserDto>> getUsersBulk(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(userService.getUsersByIds(userIds));
    }

    @GetMapping("/leads")
    public ResponseEntity<List<User>> getLeads(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.VIEW_DASHBOARD);
        return ResponseEntity.ok(userService.getLeads());
    }

    @PutMapping("/{userId}/approve")
    public ResponseEntity<User> approveMember(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable("userId") Long targetUserId,
            @RequestBody(required = false) Map<String, Object> payload) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_MEMBERS);

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomRoleDto targetCustomRole = resolveCustomRole(targetUser.getCustomRoleId());
        authorizationService.requireCanModifyTargetUser(
                authUser,
                targetUser.getRole().name(),
                targetCustomRole != null ? targetCustomRole.getPosition() : 0);

        if (payload == null) {
            payload = Map.of();
        }

        String roleStr = payload.get("role") != null
                ? (String) payload.get("role")
                : "MEMBER";

        UserRole newRole = UserRole.valueOf(roleStr);
        authorizationService.requireCanAssignRole(authUser, newRole.name());

        Long customRoleIdValue = payload.get("customRoleId") != null
                ? ((Number) payload.get("customRoleId")).longValue() : null;
        String reason = payload.get("reason") != null ? (String) payload.get("reason") : (String) payload.get("suspensionReason");

        if (newRole == UserRole.SUSPENDED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Suspension reason is required");
        }

        return ResponseEntity.ok(userService.approveMember(targetUserId, newRole, customRoleIdValue, reason));
    }

    @PutMapping("/{userId}/reject")
    public ResponseEntity<User> rejectMember(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable("userId") Long targetUserId,
            @RequestBody(required = false) Map<String, Object> payload) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_MEMBERS);

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomRoleDto targetCustomRole = resolveCustomRole(targetUser.getCustomRoleId());
        authorizationService.requireCanModifyTargetUser(
                authUser,
                targetUser.getRole().name(),
                targetCustomRole != null ? targetCustomRole.getPosition() : 0);

        String reason = payload != null && payload.get("reason") != null
                ? (String) payload.get("reason")
                : "Rejected via admin action";

        return ResponseEntity.ok(userService.approveMember(targetUserId, UserRole.SUSPENDED, null, reason));
    }

    @PutMapping("/{userId}/custom-role")
    public ResponseEntity<User> updateUserCustomRole(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable("userId") Long targetUserId,
            @RequestBody Map<String, Object> payload) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_MEMBERS);

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomRoleDto targetCustomRole = resolveCustomRole(targetUser.getCustomRoleId());
        authorizationService.requireCanModifyTargetUser(
                authUser,
                targetUser.getRole().name(),
                targetCustomRole != null ? targetCustomRole.getPosition() : 0);

        if (payload.containsKey("customRoleId")) {
            targetUser.setCustomRoleId(payload.get("customRoleId") != null
                    ? ((Number) payload.get("customRoleId")).longValue()
                    : null);
        }

        return ResponseEntity.ok(userService.saveUser(targetUser));
    }

    @DeleteMapping("/{userId}/custom-role")
    public ResponseEntity<User> removeUserCustomRole(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable("userId") Long targetUserId) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_MEMBERS);

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomRoleDto targetCustomRole = resolveCustomRole(targetUser.getCustomRoleId());
        authorizationService.requireCanModifyTargetUser(
                authUser,
                targetUser.getRole().name(),
                targetCustomRole != null ? targetCustomRole.getPosition() : 0);

        targetUser.setCustomRoleId(null);
        return ResponseEntity.ok(userService.saveUser(targetUser));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<User> updateUserRole(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable("userId") Long targetUserId,
            @RequestBody Map<String, Object> payload) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_MEMBERS);

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomRoleDto targetCustomRole = resolveCustomRole(targetUser.getCustomRoleId());
        authorizationService.requireCanModifyTargetUser(
                authUser,
                targetUser.getRole().name(),
                targetCustomRole != null ? targetCustomRole.getPosition() : 0);

        UserRole newRole = payload.containsKey("role")
                ? UserRole.valueOf((String) payload.get("role"))
                : payload.containsKey("roleName")
                ? UserRole.valueOf((String) payload.get("roleName"))
                : targetUser.getRole();
        authorizationService.requireCanAssignRole(authUser, newRole.name());

        Long customRoleIdValue = payload.containsKey("customRoleId")
                ? (payload.get("customRoleId") != null ? ((Number) payload.get("customRoleId")).longValue() : null)
                : targetUser.getCustomRoleId();
        String reason = payload.containsKey("reason") ? (String) payload.get("reason")
                : (payload.containsKey("suspensionReason") ? (String) payload.get("suspensionReason") : null);

        if (newRole == UserRole.SUSPENDED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Suspension reason is required");
        }

        return ResponseEntity.ok(userService.updateUserRole(targetUser, newRole, customRoleIdValue, reason));
    }

    @PutMapping("/{userId}/lead")
    public ResponseEntity<User> setUserLeadStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-User-Position", required = false) String position,
            @RequestHeader(value = "X-User-Is-Lead", required = false) String isLead,
            @RequestHeader(value = "X-User-Is-Active", required = false) String isActive,
            @RequestHeader(value = "X-User-Custom-Role-Id", required = false) String customRoleId,
            @PathVariable("userId") Long targetUserId,
            @RequestBody Map<String, Boolean> payload) {

        AuthUserContext authUser = authContextResolver.resolve(userId, role, permissions, position, isLead, isActive, customRoleId);
        authorizationService.requirePermission(authUser, Permission.MANAGE_MEMBERS);

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomRoleDto targetCustomRole = resolveCustomRole(targetUser.getCustomRoleId());
        authorizationService.requireCanModifyTargetUser(
                authUser,
                targetUser.getRole().name(),
                targetCustomRole != null ? targetCustomRole.getPosition() : 0);

        if (payload.containsKey("isLead")) {
            targetUser.setIsLead(payload.get("isLead"));
        }

        return ResponseEntity.ok(userService.saveUser(targetUser));
    }

    private CustomRoleDto resolveCustomRole(Long customRoleId) {
        if (customRoleId == null) {
            return null;
        }
        try {
            return roleServiceClient.getRoleById(customRoleId);
        } catch (Exception e) {
            return null;
        }
    }
}
