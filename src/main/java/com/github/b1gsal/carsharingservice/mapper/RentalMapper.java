package com.github.b1gsal.carsharingservice.mapper;

import com.github.b1gsal.carsharingservice.config.MapperConfig;
import com.github.b1gsal.carsharingservice.dto.rental.RentalRequestDto;
import com.github.b1gsal.carsharingservice.dto.rental.RentalResponseDto;
import com.github.b1gsal.carsharingservice.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface RentalMapper {
    @Mapping(target = "carId", source = "car.id")
    @Mapping(target = "userId", source = "user.id")
    RentalResponseDto toDto(Rental rental);

    Rental toModel(RentalRequestDto requestDto);
}
