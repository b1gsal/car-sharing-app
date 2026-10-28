package com.github.b1gsal.carsharingservice.dto.rental;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RentalRequestDto(
        @FutureOrPresent
        @NotNull
        LocalDate returnDate,
        @NotNull
        Long carId
) {
}
