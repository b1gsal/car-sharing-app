package com.github.b1gsal.carsharingservice.dto.car;

import com.github.b1gsal.carsharingservice.model.Type;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CarUpdateDto(
        @Pattern(regexp = "^\\S+.*$", message = "Model can't be blank")
        String model,

        @Pattern(regexp = "^\\S+.*$", message = "Brand can't be blank")
        String brand,

        Type type,

        @Positive(message = "Daily fee must be positive")
        BigDecimal dailyFee,

        @PositiveOrZero(message = "Inventory can't be negative")
        Integer inventory
) {
}
