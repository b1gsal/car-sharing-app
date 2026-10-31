package com.github.b1gsal.carsharingservice.repository.rental;

import com.github.b1gsal.carsharingservice.model.Rental;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    boolean existsByUserIdAndActualReturnDateIsNull(Long userId);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findByUserIdAndActualReturnDateIsNotNull(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findByUserIdAndActualReturnDateIsNull(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findByUserId(Long actualUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findByActualReturnDateIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findByActualReturnDateIsNotNull(Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Optional<Rental> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"car", "user"})
    List<Rental> findByActualReturnDateIsNullAndReturnDateLessThanEqual(LocalDate date);
}
