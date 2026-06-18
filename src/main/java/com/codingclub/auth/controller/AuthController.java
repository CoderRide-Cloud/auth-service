package com.codingclub.auth.controller;

import com.codingclub.auth.dto.AuthResponse;
import com.codingclub.auth.dto.GithubAuthRequest;
import com.codingclub.auth.model.User;
import com.codingclub.auth.service.GitHubService;
import com.codingclub.auth.service.UserService;
import com.codingclub.common.exception.ResourceNotFoundException;
import com.codingclub.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/github")
    public ResponseEntity<AuthResponse> githubSignIn(@RequestBody GithubAuthRequest request) {
        if (request.getGithubId() == null || request.getUsername() == null) {
            throw new IllegalArgumentException("GitHub ID and username are required");
        }

        User user = userService.getOrCreateUser(request);
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        return ResponseEntity.ok(buildAuthResponse(user, token));
    }

    @PostMapping("/github/callback")
    public ResponseEntity<AuthResponse> githubCallback(@RequestBody GithubAuthRequest request) {
        if (request.getCode() == null) {
            throw new IllegalArgumentException("Authorization code is required");
        }

        String accessToken = gitHubService.exchangeCodeForToken(request.getCode());
        GithubAuthRequest gitHubUser = gitHubService.getGitHubUser(accessToken);

        User user = userService.getOrCreateUser(gitHubUser);
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        return ResponseEntity.ok(buildAuthResponse(user, token));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getMe(@RequestHeader("X-User-Id") String userIdHeader) {
        Long userId = Long.valueOf(userIdHeader);
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(buildAuthResponse(user, null));
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .customRoleId(user.getCustomRoleId())
                .isActive(user.getIsActive())
                .isLead(user.getIsLead())
                .token(token)
                .build();
    }
}
