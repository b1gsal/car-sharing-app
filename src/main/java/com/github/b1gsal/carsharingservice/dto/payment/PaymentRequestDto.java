package com.github.b1gsal.carsharingservice.dto.payment;

import com.github.b1gsal.carsharingservice.model.Payment;
import jakarta.validation.constraints.NotNull;

public record PaymentRequestDto(
        @NotNull
        Long rentalId,
        @NotNull
        Payment.Type type) {
}
