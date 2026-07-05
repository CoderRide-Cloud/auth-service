package com.codingclub.auth.dto;

import com.codingclub.common.security.Permission;
import lombok.Data;

import java.util.Set;

import java.io.Serializable;

@Data
public class CustomRoleDto implements Serializable {
    private Long id;
    private String name;
    private Integer position;
    private Set<Permission> permissions;
}
