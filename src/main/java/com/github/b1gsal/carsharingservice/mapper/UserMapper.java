package com.github.b1gsal.carsharingservice.mapper;

import com.github.b1gsal.carsharingservice.config.MapperConfig;
import com.github.b1gsal.carsharingservice.dto.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.UserResponseDto;
import com.github.b1gsal.carsharingservice.model.User;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    User toModel(UserRegisterRequestDto requestDto);

    UserResponseDto toDto(User user);
}
