package com.codingclub.auth.controller;

import com.codingclub.auth.dto.AuthResponse;
import com.codingclub.auth.dto.GithubAuthRequest;
import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import com.codingclub.auth.service.GitHubService;
import com.codingclub.auth.service.UserService;
import com.codingclub.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private GithubAuthRequest request;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.MEMBER);

        request = new GithubAuthRequest();
        request.setGithubId("12345");
        request.setUsername("testuser");
    }

    @Test
    void testGithubSignIn() {
        when(userService.getOrCreateUser(any(GithubAuthRequest.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(1L, "MEMBER")).thenReturn("fake-jwt-token");

        ResponseEntity<AuthResponse> response = authController.githubSignIn(request);

        assertNotNull(response.getBody());
        assertEquals("fake-jwt-token", response.getBody().getToken());
        assertEquals("testuser", response.getBody().getUsername());
    }

    @Test
    void testGithubCallback() {
        request.setCode("auth-code");
        
        when(gitHubService.exchangeCodeForToken("auth-code")).thenReturn("access-token");
        when(gitHubService.getGitHubUser("access-token")).thenReturn(request);
        when(userService.getOrCreateUser(any(GithubAuthRequest.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(1L, "MEMBER")).thenReturn("fake-jwt-token");

        ResponseEntity<AuthResponse> response = authController.githubCallback(request);

        assertNotNull(response.getBody());
        assertEquals("fake-jwt-token", response.getBody().getToken());
    }
}
