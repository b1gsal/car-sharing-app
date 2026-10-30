package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.payment.PaymentRequestDto;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import com.github.b1gsal.carsharingservice.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto createPayment(
            @RequestBody @Valid PaymentRequestDto requestDto,
            Authentication authentication) {
        return paymentService.createPayment(requestDto, authentication);
    }

    @GetMapping("/success")
    public PaymentResponseDto handleSuccessPayment(@RequestParam("session_id") String sessionId) {
        return paymentService.processSuccessfulPayment(sessionId);
    }

    @GetMapping("/cancel")
    public String handleCancelPayment() {
        return "Payment can be made later. The Stripe session is valid for 24 hours";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @GetMapping
    public Page<PaymentResponseDto> getPayments(
            @RequestParam(name = "user_id", required = false) Long userId,
            Authentication authentication,
            Pageable pageable) {
        return paymentService.getPayments(authentication, pageable, userId);

    }

}
