package com.github.b1gsal.carsharingservice.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Register user - valid data - should return UserResponseDto")
    void register_validData_shouldRegisterUser() {
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto(
                "customer@example.com",
                "John",
                "Doe",
                "password123",
                "password123"
        );

        User userModel = new User();
        userModel.setEmail("customer@example.com");
        userModel.setFirstName("John");
        userModel.setLastName("Doe");

        Role role = new Role();
        role.setId(1L);
        role.setName(RoleName.ROLE_CUSTOMER);

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("customer@example.com");
        savedUser.setFirstName("John");
        savedUser.setLastName("Doe");
        savedUser.setPassword("encodedPassword");
        savedUser.setRoles(Set.of(role));

        UserResponseDto expected = new UserResponseDto(
                10L,
                "customer@example.com",
                "John",
                "Doe"
        );

        when(userRepository.existsByEmail(requestDto.email())).thenReturn(false);
        when(userMapper.toModel(requestDto)).thenReturn(userModel);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(requestDto.password())).thenReturn("encodedPassword");
        when(userRepository.save(userModel)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expected);

        UserResponseDto actual = userService.register(requestDto);

        assertThat(actual).isEqualTo(expected);

        verify(userRepository, times(1)).existsByEmail(requestDto.email());
        verify(userMapper, times(1)).toModel(requestDto);
        verify(roleRepository, times(1)).findByName(RoleName.ROLE_CUSTOMER);
        verify(passwordEncoder, times(1)).encode(requestDto.password());
        verify(userRepository, times(1)).save(userModel);
        verify(userMapper, times(1)).toDto(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Register user - existing email - should throw RegistrationException")
    void register_existingEmail_shouldThrowRegistrationException() {
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto(
                "existing@example.com",
                "John",
                "Doe",
                "password123",
                "password123"
        );

        when(userRepository.existsByEmail(requestDto.email())).thenReturn(true);

        assertThrows(RegistrationException.class, () -> userService.register(requestDto));

        verify(userRepository, times(1)).existsByEmail(requestDto.email());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Register user - role not found - should throw RegistrationException")
    void register_roleNotFound_shouldThrowRegistrationException() {
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto(
                "customer@example.com",
                "John",
                "Doe",
                "password123",
                "password123"
        );

        User userModel = new User();

        when(userRepository.existsByEmail(requestDto.email())).thenReturn(false);
        when(userMapper.toModel(requestDto)).thenReturn(userModel);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.empty());

        assertThrows(RegistrationException.class, () -> userService.register(requestDto));

        verify(userRepository, times(1)).existsByEmail(requestDto.email());
        verify(userMapper, times(1)).toModel(requestDto);
        verify(roleRepository, times(1)).findByName(RoleName.ROLE_CUSTOMER);
        verifyNoMoreInteractions(userRepository, userMapper, roleRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Get profile info - valid authentication - should return UserResponseDto")
    void getProfileInfo_validAuthentication_shouldReturnUserResponseDto() {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(10L);
        user.setEmail("customer@example.com");

        UserResponseDto expected = new UserResponseDto(
                10L,
                "customer@example.com",
                "John",
                "Doe"
        );

        when(authentication.getPrincipal()).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        UserResponseDto actual = userService.getProfileInfo(authentication);

        assertThat(actual).isEqualTo(expected);

        verify(userMapper, times(1)).toDto(user);
        verifyNoMoreInteractions(userMapper);
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Update role - valid user id and role - should return UserResponseWithRoleDto")
    void updateRole_validData_shouldUpdateRoleAndReturnDto() {
        Long userId = 10L;
        RoleNameDto roleNameDto = new RoleNameDto("ROLE_MANAGER");

        User user = new User();
        user.setId(userId);
        user.setRoles(new HashSet<>());

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.ROLE_MANAGER);

        UserResponseWithRoleDto expected = new UserResponseWithRoleDto(
                userId,
                "user@example.com",
                "John",
                "Doe",
                Set.of(managerRole)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.of(managerRole));
        when(userMapper.toDtoWithRoles(user)).thenReturn(expected);

        UserResponseWithRoleDto actual = userService.updateRole(userId, roleNameDto);

        assertThat(actual).isEqualTo(expected);
        assertThat(user.getRoles()).contains(managerRole);

        verify(userRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName(RoleName.ROLE_MANAGER);
        verify(userMapper, times(1)).toDtoWithRoles(user);
        verifyNoMoreInteractions(userRepository, roleRepository, userMapper);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Update role - invalid user id - should throw UsernameNotFoundException")
    void updateRole_invalidUserId_shouldThrowUsernameNotFoundException() {
        Long userId = 99L;
        RoleNameDto roleNameDto = new RoleNameDto("ROLE_MANAGER");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.updateRole(userId, roleNameDto));

        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(roleRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Update profile info - valid request with password - should update and return UserResponseDto")
    void updateInfo_validDataWithPassword_shouldUpdateUserAndReturnDto() {
        Authentication authentication = mock(Authentication.class);
        User principalUser = new User();
        principalUser.setEmail("customer@example.com");

        User userFromDb = new User();
        userFromDb.setId(10L);
        userFromDb.setEmail("customer@example.com");
        userFromDb.setFirstName("John");
        userFromDb.setLastName("Doe");

        UserUpdateDto updateDto = new UserUpdateDto(
                "newemail@example.com",
                "Johnny",
                "UpdatedLastName",
                "newPassword123",
                "newPassword123"
        );

        UserResponseDto expected = new UserResponseDto(
                10L,
                "newemail@example.com",
                "Johnny",
                "UpdatedLastName"
        );

        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userFromDb));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userMapper.toDto(userFromDb)).thenReturn(expected);

        doAnswer(invocation -> {
            UserUpdateDto dto = invocation.getArgument(0);
            User u = invocation.getArgument(1);
            u.setEmail(dto.email());
            u.setFirstName(dto.firstName());
            u.setLastName(dto.lastName());
            return null;
        }).when(userMapper).updateUserFromDto(updateDto, userFromDb);

        UserResponseDto actual = userService.updateInfo(authentication, updateDto);

        assertThat(actual).isEqualTo(expected);
        assertThat(userFromDb.getPassword()).isEqualTo("encodedNewPassword");

        verify(userRepository, times(1)).findByEmail("customer@example.com");
        verify(userRepository, times(1)).existsByEmail("newemail@example.com");
        verify(userMapper, times(1)).updateUserFromDto(updateDto, userFromDb);
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userMapper, times(1)).toDto(userFromDb);
        verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
        verifyNoInteractions(roleRepository);
    }

    @Test
    @DisplayName("Update profile info - email already taken - should throw RegistrationException")
    void updateInfo_takenEmail_shouldThrowRegistrationException() {
        Authentication authentication = mock(Authentication.class);
        User principalUser = new User();
        principalUser.setEmail("customer@example.com");

        User userFromDb = new User();
        userFromDb.setEmail("customer@example.com");

        UserUpdateDto updateDto = new UserUpdateDto(
                "taken@example.com",
                "Johnny",
                "Doe",
                null,
                null
        );

        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userFromDb));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(RegistrationException.class, () -> userService.updateInfo(authentication, updateDto));

        verify(userRepository, times(1)).findByEmail("customer@example.com");
        verify(userRepository, times(1)).existsByEmail("taken@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder, roleRepository);
    }

    @Test
    @DisplayName("Update role - role not found in DB - should throw UsernameNotFoundException")
    void updateRole_roleNotFound_shouldThrowUsernameNotFoundException() {
        Long userId = 10L;
        RoleNameDto roleNameDto = new RoleNameDto("ROLE_MANAGER");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.updateRole(userId, roleNameDto));

        verify(userRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName(RoleName.ROLE_MANAGER);
        verifyNoMoreInteractions(userRepository, roleRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Update role - invalid role name enum - should throw IllegalArgumentException")
    void updateRole_invalidRoleName_shouldThrowIllegalArgumentException() {
        Long userId = 10L;
        RoleNameDto roleNameDto = new RoleNameDto("INVALID_ROLE");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.updateRole(userId, roleNameDto));

        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(roleRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Update profile info - user not found in DB - should throw UsernameNotFoundException")
    void updateInfo_userNotFoundInDb_shouldThrowUsernameNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        User principalUser = new User();
        principalUser.setEmail("notfound@example.com");

        UserUpdateDto updateDto = new UserUpdateDto(null, "John", "Doe", null, null);

        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.updateInfo(authentication, updateDto));

        verify(userRepository, times(1)).findByEmail("notfound@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder, roleRepository);
    }

    @Test
    @DisplayName("Update profile info - same email provided - should not check email existence")
    void updateInfo_sameEmail_shouldNotCheckEmailExistence() {
        Authentication authentication = mock(Authentication.class);
        User principalUser = new User();
        principalUser.setEmail("customer@example.com");

        User userFromDb = new User();
        userFromDb.setId(10L);
        userFromDb.setEmail("customer@example.com");

        UserUpdateDto updateDto = new UserUpdateDto(
                "customer@example.com",
                "Johnny",
                "Doe",
                null,
                null
        );

        UserResponseDto expected = new UserResponseDto(10L, "customer@example.com", "Johnny", "Doe");

        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userFromDb));
        when(userMapper.toDto(userFromDb)).thenReturn(expected);

        UserResponseDto actual = userService.updateInfo(authentication, updateDto);

        assertThat(actual).isEqualTo(expected);

        verify(userRepository, times(1)).findByEmail("customer@example.com");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userMapper, times(1)).updateUserFromDto(updateDto, userFromDb);
        verify(userMapper, times(1)).toDto(userFromDb);
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Update profile info - null password - should not encode password")
    void updateInfo_nullPassword_shouldNotEncodePassword() {
        Authentication authentication = mock(Authentication.class);
        User principalUser = new User();
        principalUser.setEmail("customer@example.com");

        User userFromDb = new User();
        userFromDb.setId(10L);
        userFromDb.setEmail("customer@example.com");
        userFromDb.setPassword("oldEncodedPassword");

        UserUpdateDto updateDto = new UserUpdateDto(
                null,
                "Johnny",
                "Doe",
                null,
                null
        );

        UserResponseDto expected = new UserResponseDto(10L, "customer@example.com", "Johnny", "Doe");

        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userFromDb));
        when(userMapper.toDto(userFromDb)).thenReturn(expected);

        UserResponseDto actual = userService.updateInfo(authentication, updateDto);

        assertThat(actual).isEqualTo(expected);
        assertThat(userFromDb.getPassword()).isEqualTo("oldEncodedPassword");

        verify(userRepository, times(1)).findByEmail("customer@example.com");
        verify(userMapper, times(1)).updateUserFromDto(updateDto, userFromDb);
        verify(userMapper, times(1)).toDto(userFromDb);
        verifyNoInteractions(passwordEncoder);
        verifyNoMoreInteractions(userRepository, userMapper);
    }
}