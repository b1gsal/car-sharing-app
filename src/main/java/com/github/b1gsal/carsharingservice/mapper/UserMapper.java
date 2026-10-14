package com.github.b1gsal.carsharingservice.mapper;

import com.github.b1gsal.carsharingservice.config.MapperConfig;
import com.github.b1gsal.carsharingservice.dto.user.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseWithRoleDto;
import com.github.b1gsal.carsharingservice.dto.user.UserUpdateDto;
import com.github.b1gsal.carsharingservice.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    User toModel(UserRegisterRequestDto requestDto);

    UserResponseDto toDto(User user);

    UserResponseWithRoleDto toDtoWithRoles(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    void updateUserFromDto(UserUpdateDto updateDto, @MappingTarget User user);
}
