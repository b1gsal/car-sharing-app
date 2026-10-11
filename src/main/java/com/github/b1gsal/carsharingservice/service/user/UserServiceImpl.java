package com.github.b1gsal.carsharingservice.service.user;

import com.github.b1gsal.carsharingservice.dto.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.UserResponseDto;
import com.github.b1gsal.carsharingservice.exception.RegistrationException;
import com.github.b1gsal.carsharingservice.mapper.UserMapper;
import com.github.b1gsal.carsharingservice.model.Role;
import com.github.b1gsal.carsharingservice.model.RoleName;
import com.github.b1gsal.carsharingservice.model.User;
import com.github.b1gsal.carsharingservice.repository.role.RoleRepository;
import com.github.b1gsal.carsharingservice.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public UserResponseDto register(UserRegisterRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException("User with email: "
                    + requestDto.email()
                    + " already registered");
        } else {
            User model = userMapper.toModel(requestDto);
            Role role = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                    .orElseThrow(
                            () -> new RegistrationException(
                                    "Can't find role in db "
                                            + RoleName.ROLE_CUSTOMER.name()));
            model.getRoles().add(role);
            model.setPassword(passwordEncoder.encode(requestDto.password()));
            User savedUser = userRepository.save(model);
            return userMapper.toDto(savedUser);
        }
    }
}
