package com.codingclub.auth.dto;

import lombok.Data;

@Data
public class GithubAuthRequest {
    private String githubId;
    private String username;
    private String email;
    private String avatarUrl;
    private String githubUrl;
    private String code; // For OAuth callback
}
