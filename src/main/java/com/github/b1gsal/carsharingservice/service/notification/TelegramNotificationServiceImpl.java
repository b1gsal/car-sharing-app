package com.github.b1gsal.carsharingservice.service.notification;

import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.model.Rental;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Service
public class TelegramNotificationServiceImpl implements NotificationService {
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    @Override
    public void notifyRentalCreated(Rental rental) {
        String message = String.format("""
                *New Rental Created!*
                *Rental ID:* %d
                *User:* %s (%s)
                *Car:* %s %s
                *Rental Date:* %s
                *Expected Return:* %s
                """,
                rental.getId(),
                rental.getUser().getFirstName() + " " + rental.getUser().getLastName(),
                rental.getUser().getEmail(),
                rental.getCar().getBrand(),
                rental.getCar().getModel(),
                rental.getRentalDate(),
                rental.getReturnDate()
        );
        sendMessage(message);
    }

    @Override
    public void notifyPaymentSuccess(Payment payment) {
        String message = String.format("""
                *Payment Successful!*
                *Payment ID:* %d
                *Rental ID:* %d
                *Type:* %s
                *Amount:* $%s
                *User:* %s
                """,
                payment.getId(),
                payment.getRental().getId(),
                payment.getType(),
                payment.getAmountToPay(),
                payment.getRental().getUser().getEmail()
        );
        sendMessage(message);
    }

    @Override
    public void notifyOverdueRentals(List<Rental> overdueRentals) {
        if (overdueRentals.isEmpty()) {
            sendMessage("*No rentals overdue today!*");
            return;
        }

        StringBuilder builder = new StringBuilder("*Overdue Rentals Alert!*\n\n");
        for (Rental rental : overdueRentals) {
            builder.append(String.format("""
                    • *Rental ID:* %d 
                      *User:* %s
                      *Car:* %s %s 
                      *Expected Return:* %s \n
                    """,
                    rental.getId(),
                    rental.getUser().getEmail(),
                    rental.getCar().getBrand(),
                    rental.getCar().getModel(),
                    rental.getReturnDate()
            ));
        }
        sendMessage(builder.toString());
    }

    private void sendMessage(String text) {
        String url = String.format(TELEGRAM_API_URL, botToken);
        TelegramSendMessageDto request = new TelegramSendMessageDto(chatId, text, "Markdown");
        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("Failed to send Telegram notification: " + e.getMessage());
        }

    }

    private record TelegramSendMessageDto(
            String chat_id,
            String text,
            String parse_mode
    ) {}
}
