package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.role.RoleNameDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseWithRoleDto;
import com.github.b1gsal.carsharingservice.dto.user.UserUpdateDto;
import com.github.b1gsal.carsharingservice.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @GetMapping("/me")
    public UserResponseDto getProfileInfo(
            Authentication authentication) {
        return userService.getProfileInfo(authentication);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}/role")
    public UserResponseWithRoleDto updateRoleInUser(
            @PathVariable Long id,
            @RequestBody RoleNameDto roleNameDto) {
        return userService.updateRole(id, roleNameDto);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @RequestMapping(value = "/me", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public UserResponseDto updateProfileInfo(
            Authentication authentication,
            @RequestBody @Valid UserUpdateDto updateDto) {
        return userService.updateInfo(authentication, updateDto);
    }
}
