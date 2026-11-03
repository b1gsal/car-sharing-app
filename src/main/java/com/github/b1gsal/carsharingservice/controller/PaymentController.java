package com.github.b1gsal.carsharingservice.controller;

import com.github.b1gsal.carsharingservice.dto.payment.PaymentRequestDto;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import com.github.b1gsal.carsharingservice.service.payment.PaymentService;
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

@Tag(name = "Payment Management",
        description = "Endpoints for handling Stripe payment sessions")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Create payment session",
            description = "Generates a Stripe checkout session for a rental")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto createPayment(
            @RequestBody @Valid PaymentRequestDto requestDto,
            Authentication authentication) {
        return paymentService.createPayment(requestDto, authentication);
    }

    @Operation(summary = "Process successful payment",
            description = "Redirection endpoint after successful Stripe payment")
    @GetMapping("/success")
    public PaymentResponseDto handleSuccessPayment(@RequestParam("session_id") String sessionId) {
        return paymentService.processSuccessfulPayment(sessionId);
    }

    @Operation(summary = "Cancel payment notification",
            description = "Redirection endpoint when payment is canceled on Stripe")
    @GetMapping("/cancel")
    public String handleCancelPayment() {
        return "Payment can be made later. The Stripe session is valid for 24 hours";
    }

    @Operation(summary = "Get payments list",
            description = "Retrieves payment records for user or manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @GetMapping
    public Page<PaymentResponseDto> getPayments(
            @RequestParam(name = "user_id", required = false) Long userId,
            Authentication authentication,
            @ParameterObject Pageable pageable) {
        return paymentService.getPayments(authentication, pageable, userId);
    }

    @Operation(summary = "Renew expired payment",
            description = "Re-creates a Stripe checkout session for expired payments")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @PostMapping("/{id}/renew")
    public PaymentResponseDto renewPayment(@PathVariable Long id, Authentication authentication) {
        return paymentService.renewPayment(id, authentication);
    }
}
