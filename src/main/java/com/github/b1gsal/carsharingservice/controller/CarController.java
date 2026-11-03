package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import com.github.b1gsal.carsharingservice.service.car.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

@Tag(name = "Car Management",
        description = "Endpoints for managing cars inventory")
@RequiredArgsConstructor
@RestController
@RequestMapping("/cars")
public class CarController {
    private final CarService carService;

    @Operation(summary = "Get all cars",
            description = "Retrieves a paginated list of all available cars")
    @PermitAll
    @GetMapping
    public Page<CarResponseDto> getCars(@ParameterObject Pageable pageable) {
        return carService.getCars(pageable);
    }

    @Operation(summary = "Add a new car",
            description = "Creates a new car entry (Manager only)")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CarResponseDto addNewCar(@RequestBody @Valid CarRequestDto requestDto) {
        return carService.addCar(requestDto);
    }

    @Operation(summary = "Get car by ID",
            description = "Retrieves detailed information about a specific car")
    @PermitAll
    @GetMapping("/{id}")
    public CarResponseDto getDetailedCarInfo(@PathVariable Long id) {
        return carService.getCarInfo(id);
    }

    @Operation(summary = "Delete car by ID",
            description = "Soft deletes a car from the database (Manager only)")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        carService.deleteById(id);
    }

    @Operation(summary = "Update car details",
            description = "Updates car information by ID (Manager only)")
    @PreAuthorize("hasRole('MANAGER')")
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public CarResponseDto updateCar(
            @RequestBody @Valid CarUpdateDto requestDto,
            @PathVariable Long id) {
        return carService.update(requestDto, id);
    }
}
