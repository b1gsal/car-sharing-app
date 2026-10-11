package com.github.b1gsal.carsharingservice.dto;

import com.github.b1gsal.carsharingservice.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@FieldMatch(
        first = "password",
        second = "repeatPassword"
)
public record UserRegisterRequestDto(
        @Email
        @NotBlank
        String email,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String password,
        @NotBlank
        String repeatPassword) {
}
