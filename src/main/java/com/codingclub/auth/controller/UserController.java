package com.codingclub.auth.controller;

import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import com.codingclub.auth.service.UserService;
import com.codingclub.common.exception.ResourceNotFoundException;
import com.codingclub.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Helper method to check if requester is ADMIN
    private void verifyAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<User> updateUserRole(
            @RequestHeader("X-User-Role") String requesterRole,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> payload) {
        
        verifyAdmin(requesterRole);

        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (payload.containsKey("role")) {
            user.setRole(UserRole.valueOf((String) payload.get("role")));
        }
        if (payload.containsKey("customRoleId")) {
            user.setCustomRoleId(payload.get("customRoleId") != null ? ((Number) payload.get("customRoleId")).longValue() : null);
        }
        if (payload.containsKey("suspensionReason")) {
            user.setSuspensionReason((String) payload.get("suspensionReason"));
        }
        if (payload.containsKey("isActive")) {
            user.setIsActive((Boolean) payload.get("isActive"));
        }

        return ResponseEntity.ok(userService.saveUser(user));
    }

    @PutMapping("/{userId}/lead")
    public ResponseEntity<User> setUserLeadStatus(
            @RequestHeader("X-User-Role") String requesterRole,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> payload) {
        
        verifyAdmin(requesterRole);

        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (payload.containsKey("isLead")) {
            user.setIsLead(payload.get("isLead"));
        }

        return ResponseEntity.ok(userService.saveUser(user));
    }
}
