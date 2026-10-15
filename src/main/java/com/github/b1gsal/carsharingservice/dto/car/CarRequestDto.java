package com.github.b1gsal.carsharingservice.dto.car;

import com.github.b1gsal.carsharingservice.model.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CarRequestDto(
        @NotBlank
        String model,
        @NotBlank
        String brand,
        @NotNull
        Type type,
        @NotNull
        @Positive
        BigDecimal dailyFee,
        @NotNull
        @PositiveOrZero
        Integer inventory
) {
}
