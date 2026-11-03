package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.rental.RentalRequestDto;
import com.github.b1gsal.carsharingservice.dto.rental.RentalResponseDto;
import com.github.b1gsal.carsharingservice.service.rental.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rental Management", description = "Endpoints for managing car rentals")
@RequiredArgsConstructor
@RestController
@RequestMapping("/rentals")
public class RentalController {
    private final RentalService rentalService;

    @Operation(summary = "Create a new rental", description = "Creates a rental for a specific car")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponseDto rentCar(
            @RequestBody @Valid RentalRequestDto requestDto,
            Authentication authentication) {
        return rentalService.rentCar(requestDto, authentication);
    }

    @Operation(summary = "Return a rented car",
            description = "Sets actual return date and increases car inventory")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @PostMapping("/{id}/return")
    public RentalResponseDto returnCar(@PathVariable Long id, Authentication authentication) {
        return rentalService.returnCar(authentication, id);
    }

    @Operation(summary = "Get rental by ID",
            description = "Retrieves rental details by ID")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @GetMapping("/{id}")
    public RentalResponseDto getRental(@PathVariable Long id, Authentication authentication) {
        return rentalService.getRental(authentication, id);
    }

    @Operation(summary = "Get rentals list",
            description = "Retrieves rentals filtered by user ID and active status")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @GetMapping
    public Page<RentalResponseDto> getRentals(
            @RequestParam(name = "user_id", required = false) Long userId,
            @RequestParam(name = "is_active", required = false) Boolean isActive,
            @ParameterObject Pageable pageable,
            Authentication authentication) {
        return rentalService.getRentals(userId, isActive, pageable, authentication);
    }
}
