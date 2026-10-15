package com.github.b1gsal.carsharingservice.mapper;

import com.github.b1gsal.carsharingservice.config.MapperConfig;
import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import com.github.b1gsal.carsharingservice.model.Car;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface CarMapper {
    CarResponseDto toDto(Car car);

    Car toModel(CarRequestDto requestDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCarFromDto(@MappingTarget Car car, CarUpdateDto requestDto);
}
