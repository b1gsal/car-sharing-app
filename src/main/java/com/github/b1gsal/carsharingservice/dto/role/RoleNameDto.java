package com.github.b1gsal.carsharingservice.dto.role;

import jakarta.validation.constraints.NotBlank;

public record RoleNameDto(@NotBlank String roleName) {
}
