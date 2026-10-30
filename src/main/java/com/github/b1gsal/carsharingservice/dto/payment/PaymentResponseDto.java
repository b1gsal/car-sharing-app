package com.github.b1gsal.carsharingservice.dto.payment;

import com.github.b1gsal.carsharingservice.model.Payment;
import java.math.BigDecimal;

public record PaymentResponseDto(
        Long id,
        Payment.Status status,
        Payment.Type type,
        Long rentalId,
        String sessionUrl,
        String sessionId,
        BigDecimal amountToPay) {
}
