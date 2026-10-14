package com.github.b1gsal.carsharingservice.dto.user;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName) {
}
