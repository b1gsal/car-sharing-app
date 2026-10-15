package com.github.b1gsal.carsharingservice.dto.car;

import com.github.b1gsal.carsharingservice.model.Type;
import java.math.BigDecimal;

public record CarResponseDto(
        Long id,
        String model,
        String brand,
        Type type,
        BigDecimal dailyFee,
        Integer inventory
) {
}
