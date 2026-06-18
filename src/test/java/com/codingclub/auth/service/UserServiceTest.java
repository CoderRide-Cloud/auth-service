package com.codingclub.auth.service;

import com.codingclub.auth.dto.GithubAuthRequest;
import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import com.codingclub.auth.repository.UserRepository;
import com.codingclub.common.event.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private GithubAuthRequest authRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setGithubId("12345");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.MEMBER);

        authRequest = new GithubAuthRequest();
        authRequest.setGithubId("12345");
        authRequest.setUsername("testuser");
        authRequest.setEmail("test@example.com");
    }

    @Test
    void testGetOrCreateUser_ExistingUser() {
        // Arrange
        when(userRepository.findByGithubId("12345")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getOrCreateUser(authRequest);

        // Assert
        assertNotNull(result);
        assertEquals("12345", result.getGithubId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, never()).save(any(User.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(UserCreatedEvent.class));
    }

    @Test
    void testGetOrCreateUser_NewUser() {
        // Arrange
        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        
        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setGithubId("12345");
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setRole(UserRole.MEMBER);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.getOrCreateUser(authRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("12345", result.getGithubId());
        
        verify(userRepository, times(1)).save(any(User.class));
        
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("user-created-topic"), eq("2"), eventCaptor.capture());
        
        UserCreatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(2L, capturedEvent.getUserId());
        assertEquals("testuser", capturedEvent.getUsername());
    }

    @Test
    void testFindById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        Optional<User> result = userService.findById(1L);
        
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }
}
