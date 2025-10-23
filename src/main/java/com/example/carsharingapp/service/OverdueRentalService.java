package com.example.carsharingapp.service;

import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.repository.RentalRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OverdueRentalService {
    private static final String OVERDUE_MESSAGE_HEADER = "🚨 DAILY OVERDUE RENTAL REPORT 🚨\n\n";
    private static final String NO_OVERDUE_MESSAGE = "✅ No rentals overdue today!";

    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${rental.overdue.check.cron}")
    public void checkOverdueRentals() {
        List<Rental> overdueRentals = rentalRepository.findOverdueRentals();

        if (overdueRentals.isEmpty()) {
            notificationService.sendMessage(NO_OVERDUE_MESSAGE);
            return;
        }
        String consolidatedMessage = buildOverdueMessage(overdueRentals);
        notificationService.sendMessage(consolidatedMessage);
    }

    private String buildOverdueMessage(List<Rental> rentals) {
        StringBuilder message = new StringBuilder(OVERDUE_MESSAGE_HEADER);
        for (Rental rental : rentals) {
            String rentalDetail = String.format(
                    "🔢 *Rental ID*: %d\n"
                            + "👤 User: %s %s (ID: %d, Email: %s)\n"
                            + "🚗 Car: %s %s (ID: %d)\n"
                            + "📅 Expected Return: %s\n\n",
                    rental.getId(),
                    escapeMarkdown(rental.getUser().getFirstName()),
                    escapeMarkdown(rental.getUser().getLastName()),
                    rental.getUser().getId(),
                    escapeMarkdown(rental.getUser().getEmail()),
                    escapeMarkdown(rental.getCar().getBrand()),
                    escapeMarkdown(rental.getCar().getModel()),
                    rental.getCar().getId(),
                    rental.getReturnDate()
            );
            message.append(rentalDetail);
        }
        message.append("\n⋯⋯⋯⋯⋯⋯⋯⋯⋯⋯⋯\n");
        message.append("📊 END OF REPORT (%d total)".formatted(rentals.size()));
        return message.toString();
    }

    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("([_*>\\[\\]()~`#+\\-=|{}!])", "\\\\$1");
    }
}
