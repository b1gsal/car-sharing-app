package com.github.b1gsal.carsharingservice.service.payment;

import com.github.b1gsal.carsharingservice.dto.payment.PaymentRequestDto;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface PaymentService {
    PaymentResponseDto createPayment(
            PaymentRequestDto requestDto,
            Authentication authentication);

    PaymentResponseDto processSuccessfulPayment(String sessionId);

    Page<PaymentResponseDto> getPayments(
            Authentication authentication,
            Pageable pageable,
            Long userId);
}
