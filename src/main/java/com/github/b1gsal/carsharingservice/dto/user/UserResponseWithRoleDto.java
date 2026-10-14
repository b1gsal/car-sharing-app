package com.github.b1gsal.carsharingservice.dto.user;

import com.github.b1gsal.carsharingservice.model.Role;
import java.util.Set;

public record UserResponseWithRoleDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles) {
}
