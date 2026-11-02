package com.github.b1gsal.carsharingservice.scheduler;

import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.repository.payment.PaymentRepository;
import com.github.b1gsal.carsharingservice.service.payment.PaymentService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ExpiredPaymentScheduler {
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Transactional
    @Scheduled(cron = "0 0/15 * * * *")
    public void cancelExpiredPayment() {
        LocalDateTime expiredThreshold = LocalDateTime.now().minusHours(24);
        List<Payment> expiredPayments = paymentRepository
                .findByStatusAndCreatedAtBefore(Payment.Status.PENDING, expiredThreshold);

        for (Payment payment : expiredPayments) {
            paymentService.checkAndUpdateStatus(payment);
        }
    }
}
