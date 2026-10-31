package com.github.b1gsal.carsharingservice.scheduler;

import com.github.b1gsal.carsharingservice.model.Rental;
import com.github.b1gsal.carsharingservice.repository.rental.RentalRepository;
import com.github.b1gsal.carsharingservice.service.notification.NotificationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OverdueRentalScheduler {
    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 10 * * *")
    public void checkoutOverdueRentals() {
        List<Rental> rentals = rentalRepository
                .findByActualReturnDateIsNullAndReturnDateLessThanEqual(LocalDate.now());
        notificationService.notifyOverdueRentals(rentals);
    }
}
