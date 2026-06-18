package com.codingclub.auth.dto;

import com.codingclub.auth.model.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private UserRole role;
    private Long customRoleId;
    private Boolean isActive;
    private Boolean isLead;
    private String token;
}
