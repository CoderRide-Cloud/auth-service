package com.codingclub.auth.service;

import com.codingclub.auth.dto.GitHubRepoDto;
import com.codingclub.auth.dto.GithubAuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GitHubService {

    // OPTIMIZED: Injected pooled RestTemplate bean from HttpClientConfig
    // Previously: private final RestTemplate restTemplate = new RestTemplate();
    // — no connection pooling, no timeouts, new socket on every call.
    @Autowired
    private RestTemplate restTemplate;

    @Value("${github.client-id:YOUR_CLIENT_ID}")
    private String clientId;

    @Value("${github.client-secret:YOUR_CLIENT_SECRET}")
    private String clientSecret;

    public String exchangeCodeForToken(String code) {
        String url = "https://github.com/login/oauth/access_token?client_id=" + clientId +
                "&client_secret=" + clientSecret + "&code=" + code;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        if (response.getBody() != null && response.getBody().has("access_token")) {
            return response.getBody().get("access_token").asText();
        }
        throw new RuntimeException("Failed to exchange GitHub code for token");
    }

    public GithubAuthRequest getGitHubUser(String accessToken) {
        String url = "https://api.github.com/user";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        JsonNode body = response.getBody();

        if (body != null) {
            GithubAuthRequest request = new GithubAuthRequest();
            request.setGithubId(body.get("id").asText());
            request.setUsername(body.get("login").asText());
            if (body.has("email") && !body.get("email").isNull()) {
                request.setEmail(body.get("email").asText());
            }
            if (body.has("avatar_url") && !body.get("avatar_url").isNull()) {
                request.setAvatarUrl(body.get("avatar_url").asText());
            }
            if (body.has("html_url") && !body.get("html_url").isNull()) {
                request.setGithubUrl(body.get("html_url").asText());
            }
            return request;
        }
        throw new RuntimeException("Failed to fetch GitHub user profile");
    }

    public List<GitHubRepoDto> getUserRepos(String username) {
        String url = "https://api.github.com/users/" + username + "/repos?sort=updated&per_page=100";
        ResponseEntity<List<GitHubRepoDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }
}
