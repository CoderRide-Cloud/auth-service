package com.codingclub.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDto {
    private Long id;
    private String name;
    private String full_name;
    private String description;
    private String html_url;
    private String homepage;
    private String language;
}
