package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.UserLoginRequestDto;
import com.github.b1gsal.carsharingservice.dto.UserLoginResponseDto;
import com.github.b1gsal.carsharingservice.dto.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.UserResponseDto;
import com.github.b1gsal.carsharingservice.security.AuthentificationService;
import com.github.b1gsal.carsharingservice.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final UserService userService;
    private final AuthentificationService authentificationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/registration")
    public UserResponseDto register(@RequestBody @Valid UserRegisterRequestDto requestDto) {
        return userService.register(requestDto);
    }

    @PostMapping("/login")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto requestDto) {
        return authentificationService.authenticate(requestDto);
    }
}
