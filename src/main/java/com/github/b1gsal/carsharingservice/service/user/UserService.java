package com.github.b1gsal.carsharingservice.service.user;

import com.github.b1gsal.carsharingservice.dto.role.RoleNameDto;
import com.github.b1gsal.carsharingservice.dto.user.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseWithRoleDto;
import com.github.b1gsal.carsharingservice.dto.user.UserUpdateDto;
import org.springframework.security.core.Authentication;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto requestDto);

    UserResponseDto getProfileInfo(Authentication authentication);

    UserResponseWithRoleDto updateRole(Long id, RoleNameDto roleNameDto);

    UserResponseDto updateInfo(Authentication authentication, UserUpdateDto updateDto);
}
