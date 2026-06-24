package com.codingclub.auth.repository;

import com.codingclub.auth.model.User;
import com.codingclub.auth.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGithubId(String githubId);
    Optional<User> findByUsername(String username);
    List<User> findByRoleOrderByCreatedAtDesc(UserRole role);
    List<User> findByIsLeadTrueAndIsActiveTrueAndRole(UserRole role);
    List<User> findByCustomRoleId(Long customRoleId);
}
