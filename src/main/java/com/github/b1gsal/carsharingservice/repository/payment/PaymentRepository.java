package com.github.b1gsal.carsharingservice.repository.payment;

import com.github.b1gsal.carsharingservice.model.Payment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByRentalIdAndTypeAndStatus(
            Long rentalId,
            Payment.Type type,
            Payment.Status status);

    boolean existsByRentalUserIdAndStatus(Long userId, Payment.Status status);

    Optional<Payment> findByRentalIdAndTypeAndStatus(
            Long rentalId,
            Payment.Type type,
            Payment.Status status);

    Optional<Payment> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = {"rental"})
    Page<Payment> findByRentalUserId(Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"rental"})
    Page<Payment> findAll(Pageable pageable);
}
