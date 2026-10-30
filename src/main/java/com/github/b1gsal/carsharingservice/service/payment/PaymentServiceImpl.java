package com.github.b1gsal.carsharingservice.service.payment;

import com.github.b1gsal.carsharingservice.dto.payment.PaymentRequestDto;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import com.github.b1gsal.carsharingservice.exception.EntityNotFoundException;
import com.github.b1gsal.carsharingservice.exception.PaymentException;
import com.github.b1gsal.carsharingservice.mapper.PaymentMapper;
import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.model.Rental;
import com.github.b1gsal.carsharingservice.model.RoleName;
import com.github.b1gsal.carsharingservice.model.User;
import com.github.b1gsal.carsharingservice.repository.payment.PaymentRepository;
import com.github.b1gsal.carsharingservice.repository.rental.RentalRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Value("${payment.stripe.key}")
    private String key;
    @Value("${payment.fine.multiplier}")
    private BigDecimal fineMultiplier;

    private final RentalRepository rentalRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = key;
    }

    @Transactional
    @Override
    public PaymentResponseDto createPayment(
            PaymentRequestDto requestDto, Authentication authentication) {
        if (paymentRepository.existsByRentalIdAndTypeAndStatus(
                requestDto.rentalId(), requestDto.type(), Payment.Status.PAID)) {
            throw new PaymentException("Payment for this rental has already been completed");
        }

        Optional<Payment> pendingPayment = paymentRepository.findByRentalIdAndTypeAndStatus(
                requestDto.rentalId(), requestDto.type(), Payment.Status.PENDING);
        if (pendingPayment.isPresent()) {
            return paymentMapper.toDto(pendingPayment.get());
        }

        User currentUser = (User) authentication.getPrincipal();

        Rental rental = rentalRepository.findById(requestDto.rentalId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find rental with id " + requestDto.rentalId()));

        boolean isManager = currentUser.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority()
                        .equals(RoleName.ROLE_MANAGER.name()));

        if (!isManager && !rental.getUser().getId().equals(currentUser.getId())) {
            throw new PaymentException("You don't have permission to pay this rental");
        }

        BigDecimal amountToPay = calculateAmountToPay(rental, requestDto.type());

        try {
            Session session = createStripeSession(rental, amountToPay, requestDto.type());

            Payment payment = new Payment();
            payment.setRental(rental);
            payment.setSessionId(session.getId());
            payment.setSessionUrl(session.getUrl());
            payment.setAmountToPay(amountToPay);
            payment.setStatus(Payment.Status.PENDING);
            payment.setType(requestDto.type());

            Payment savedPayment = paymentRepository.save(payment);
            return paymentMapper.toDto(savedPayment);
        } catch (StripeException e) {
            throw new PaymentException("Failed to create stripe payment");
        }
    }

    @Transactional
    @Override
    public PaymentResponseDto processSuccessfulPayment(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find payment with sessionId " + sessionId));

        try {
            Session session = Session.retrieve(sessionId);
            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                payment.setStatus(Payment.Status.PAID);
                return paymentMapper.toDto(paymentRepository.save(payment));
            }
            throw new PaymentException("Payment hasn't been completed on Stripe side");
        } catch (StripeException e) {
            throw new PaymentException("Failed to verify payment status");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PaymentResponseDto> getPayments(
            Authentication authentication, Pageable pageable, Long userId) {
        User currentUser = (User) authentication.getPrincipal();
        boolean isManager = currentUser.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(RoleName.ROLE_MANAGER.name()));

        Long targetUserId = isManager ? userId : currentUser.getId();
        Page<Payment> payments = (targetUserId != null)
                ? paymentRepository.findByRentalUserId(targetUserId, pageable)
                : paymentRepository.findAll(pageable);

        return payments.map(paymentMapper::toDto);
    }

    private Session createStripeSession(Rental rental, BigDecimal amountToPay, Payment.Type type)
            throws StripeException {
        long centsAmount = amountToPay.multiply(BigDecimal.valueOf(100)).longValue();
        String productName = "Car Rental(" + type + "): "
                + rental.getCar().getBrand() + " "
                + rental.getCar().getModel();

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(productName).build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(centsAmount)
                        .setProductData(productData).build();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:8080/payments/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:8080/payments/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(priceData).build()
                ).build();

        return Session.create(params);
    }

    private BigDecimal calculateAmountToPay(Rental rental, Payment.Type type) {
        BigDecimal dailyFee = rental.getCar().getDailyFee();

        if (type == Payment.Type.PAYMENT) {
            long days = ChronoUnit.DAYS.between(
                    rental.getRentalDate(),
                    rental.getReturnDate());
            if (days < 1) {
                days = 1;
            }
            return dailyFee.multiply(BigDecimal.valueOf(days));
        } else {
            if (rental.getActualReturnDate() == null) {
                throw new PaymentException("Can't calculate fine for unreturned rental");
            }
            long overdueDays = ChronoUnit.DAYS.between(
                    rental.getReturnDate(),
                    rental.getActualReturnDate());
            if (overdueDays <= 0) {
                throw new PaymentException("The rental isn't overdue");
            }
            return dailyFee
                    .multiply(BigDecimal.valueOf(overdueDays))
                    .multiply(fineMultiplier);
        }
    }
}
