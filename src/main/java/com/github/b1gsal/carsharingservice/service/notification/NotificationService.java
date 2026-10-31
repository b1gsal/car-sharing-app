package com.github.b1gsal.carsharingservice.service.notification;

import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.model.Rental;
import java.util.List;

public interface NotificationService {
    void notifyRentalCreated(Rental rental);

    void notifyPaymentSuccess(Payment payment);

    void notifyOverdueRentals(List<Rental> overdueRentals);
}
