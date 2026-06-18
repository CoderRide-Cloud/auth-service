package com.codingclub.auth.service;

import com.codingclub.auth.dto.GithubAuthRequest;
import com.codingclub.auth.model.User;
import com.codingclub.auth.repository.UserRepository;
import com.codingclub.common.event.UserCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_CREATED_TOPIC = "user-created-topic";

    public Optional<User> findByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User getOrCreateUser(GithubAuthRequest request) {
        Optional<User> existingUser = userRepository.findByGithubId(request.getGithubId());

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create new user
        User newUser = new User();
        newUser.setGithubId(request.getGithubId());
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setAvatarUrl(request.getAvatarUrl());
        newUser.setGithubUrl(request.getGithubUrl());
        
        User savedUser = userRepository.save(newUser);

        // Publish event for MemberService to pick up
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
}
