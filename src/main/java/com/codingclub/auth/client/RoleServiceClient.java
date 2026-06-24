package com.codingclub.auth.client;

import com.codingclub.auth.dto.CustomRoleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "role-service")
public interface RoleServiceClient {

    @GetMapping("/api/v1/roles/{id}")
    CustomRoleDto getRoleById(@PathVariable("id") Long id);
}
