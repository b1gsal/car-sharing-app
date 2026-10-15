package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import com.github.b1gsal.carsharingservice.service.car.CarService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cars")
public class CarController {
    private final CarService carService;

    @PermitAll
    @GetMapping
    public Page<CarResponseDto> getCars(Pageable pageable) {
        return carService.getCars(pageable);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CarResponseDto addNewCar(@RequestBody @Valid CarRequestDto requestDto) {
        return carService.addCar(requestDto);
    }

    @PermitAll
    @GetMapping("/{id}")
    public CarResponseDto getDetailedCarInfo(@PathVariable Long id) {
        return carService.getCarInfo(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        carService.deleteById(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public CarResponseDto updateCar(
            @RequestBody @Valid CarUpdateDto requestDto,
            @PathVariable Long id) {
        return carService.update(requestDto, id);
    }
}
