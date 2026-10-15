package com.github.b1gsal.carsharingservice.service.car;

import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {
    Page<CarResponseDto> getCars(Pageable pageable);

    CarResponseDto addCar(CarRequestDto requestDto);

    CarResponseDto getCarInfo(Long id);

    void deleteById(Long id);

    CarResponseDto update(CarUpdateDto requestDto, Long id);
}
