package com.github.b1gsal.carsharingservice.service.user;

import com.github.b1gsal.carsharingservice.dto.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.UserResponseDto;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto requestDto);
}
