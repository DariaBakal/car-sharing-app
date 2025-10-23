package com.example.carsharingapp.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.model.Car.Type;
import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.model.User.Role;
import com.example.carsharingapp.repository.RentalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverdueRentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OverdueRentalService overdueRentalService;

    private Rental rental;

    @BeforeEach
    void setUp() {
        User user = new User()
                .setId(10L)
                .setEmail("jane.doe@test.com")
                .setFirstName("Jane")
                .setLastName("Doe")
                .setPassword("dummy_password")
                .setRole(Role.CUSTOMER);

        Car car = new Car()
                .setId(1L)
                .setBrand("Toyota")
                .setModel("Corolla")
                .setType(Type.SEDAN)
                .setInventory(5)
                .setDailyFee(new BigDecimal("50.00"));

        rental = new Rental()
                .setId(100L)
                .setRentalDate(LocalDate.now().minusDays(5))
                .setReturnDate(LocalDate.now().minusDays(1))
                .setActualReturnDate(null)
                .setUser(user)
                .setCar(car);
    }

    @Test
    @DisplayName("Verify checkOverdueRentals when no overdue  rentals sends 'No Overdue' message")
    void checkOverdueRentals_NoOverdueRentals_SendsNoOverdueMessage() {
        when(rentalRepository.findOverdueRentals()).thenReturn(Collections.emptyList());

        overdueRentalService.checkOverdueRentals();

        verify(rentalRepository).findOverdueRentals();
        verify(notificationService).sendMessage("✅ No rentals overdue today!");
    }

    @Test
    @DisplayName("Verify checkOverdueRentals when one rental is overdue sends a "
            + "consolidated report with details")
    void checkOverdueRentals_WithOverdueRentals_SendsConsolidatedReport() {
        List<Rental> overdueList = List.of(rental);
        when(rentalRepository.findOverdueRentals()).thenReturn(overdueList);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        overdueRentalService.checkOverdueRentals();

        verify(rentalRepository).findOverdueRentals();
        verify(notificationService).sendMessage(messageCaptor.capture());

        String actualMessage = messageCaptor.getValue();

        assertTrue(actualMessage.contains("🚨 DAILY OVERDUE RENTAL REPORT 🚨"),
                "Message should contain the header.");
        assertTrue(actualMessage.contains("🔢 *Rental ID*: 100"),
                "Message should contain the Rental ID.");
        assertTrue(actualMessage.contains("👤 User: Jane Doe (ID: 10, Email: jane.doe@test.com)"),
                "Message should contain User details.");
        assertTrue(actualMessage.contains("🚗 Car: Toyota Corolla (ID: 1)"),
                "Message should contain Car details.");
        assertTrue(actualMessage.contains("📅 Expected Return: " + rental.getReturnDate()),
                "Message should contain the return date.");
        assertTrue(actualMessage.contains("📊 END OF REPORT (1 total)"),
                "Message should contain the report summary.");
    }
}
