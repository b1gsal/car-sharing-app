package com.github.b1gsal.carsharingservice.service.user;

import com.github.b1gsal.carsharingservice.dto.role.RoleNameDto;
import com.github.b1gsal.carsharingservice.dto.user.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseWithRoleDto;
import com.github.b1gsal.carsharingservice.dto.user.UserUpdateDto;
import com.github.b1gsal.carsharingservice.exception.RegistrationException;
import com.github.b1gsal.carsharingservice.mapper.UserMapper;
import com.github.b1gsal.carsharingservice.model.Role;
import com.github.b1gsal.carsharingservice.model.RoleName;
import com.github.b1gsal.carsharingservice.model.User;
import com.github.b1gsal.carsharingservice.repository.role.RoleRepository;
import com.github.b1gsal.carsharingservice.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @Override
    public UserResponseDto getProfileInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    public UserResponseWithRoleDto updateRole(Long id, RoleNameDto roleNameDto) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException("Can't find user with id " + id));
        RoleName roleName = RoleName.valueOf(roleNameDto.roleName());
        Role role = roleRepository.findByName(roleName).orElseThrow(
                () -> new UsernameNotFoundException("Can't find role by role name " + roleName));
        user.getRoles().clear();
        user.getRoles().add(role);
        return userMapper.toDtoWithRoles(user);
    }

    @Transactional
    @Override
    public UserResponseDto updateInfo(Authentication authentication, UserUpdateDto updateDto) {
        User user = (User) authentication.getPrincipal();
        User userFromDb = userRepository.findByEmail(user.getEmail()).orElseThrow(
                () -> new UsernameNotFoundException(
                        "Can't find user with email " + user.getEmail()));
        if (updateDto.email() != null
                && !updateDto.email().equals(userFromDb.getEmail())
                && userRepository.existsByEmail(updateDto.email())) {
            throw new RegistrationException("Email " + updateDto.email() + " is already taken");
        }
        userMapper.updateUserFromDto(updateDto, userFromDb);
        
        if (updateDto.password() != null) {
            userFromDb.setPassword(passwordEncoder.encode(updateDto.password()));
        }
        return userMapper.toDto(userFromDb);
    }

}
