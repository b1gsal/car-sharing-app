package com.github.b1gsal.carsharingservice.dto.user;

import com.github.b1gsal.carsharingservice.validation.FieldMatch;
import jakarta.validation.constraints.Pattern;

@FieldMatch(
        first = "password",
        second = "repeatPassword"
)
public record UserUpdateDto(
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
                message = "Email must be valid"
        )
        String email,
        @Pattern(
                regexp = "^\\S+.*$",
                message = "First name can't be empty or blank"
        )
        String firstName,
        @Pattern(
                regexp = "^\\S+.*$",
                message = "Last name can't be empty or blank"
        )
        String lastName,
        @Pattern(
                regexp = "^\\S+.*$",
                message = "Password can't be empty or blank"
        )
        String password,
        String repeatPassword
) {
}
