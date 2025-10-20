package com.example.carsharingapp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class RentalRepositoryTest {
    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Car car1;
    private Car car2;

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("TestFirst");
        user.setLastName("TestLast");
        user.setPassword("mockHashedPassword");
        user.setRole(User.Role.CUSTOMER);
        return entityManager.persist(user);
    }

    private Car createCar(String model, String brand, Car.Type type, BigDecimal dailyFee) {
        Car car = new Car();
        car.setModel(model);
        car.setBrand(brand);
        car.setType(type);
        car.setDailyFee(dailyFee);
        car.setInventory(1);
        return entityManager.persist(car);
    }

    private Rental createRental(User user, Car car, LocalDate returnDate,
            LocalDate actualReturnDate) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(returnDate);
        rental.setActualReturnDate(actualReturnDate);
        return entityManager.persist(rental);
    }

    @BeforeEach
    void setupAndCleanDb() {
        rentalRepository.deleteAllInBatch();

        user1 = createUser("user1@test.com");
        user2 = createUser("user2@test.com");
        car1 = createCar("Camry", "Toyota", Car.Type.SEDAN, BigDecimal.valueOf(50.00));
        car2 = createCar("HR-V", "Honda", Car.Type.SUV, BigDecimal.valueOf(80.00));

        createRental(user1, car1, LocalDate.now().plusDays(5), null);
        createRental(user1, car2, LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(2));
        createRental(user2, car1, LocalDate.now().minusDays(1), null);
        createRental(user2, car2, LocalDate.now().plusDays(10), null);
        createRental(user2, car2, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(1));
        createRental(user1, car1, LocalDate.now().minusYears(1), null);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("""
            Verify findAllByUserIdAndActualReturnDateStatus returns all rentals
            when both userId and isActive status filters are null.
            """)
    void findAllByUserIdAndActualReturnDateStatus_AllFiltersNull_ReturnsAllRentals() {
        Page<Rental> result = rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                null,
                null,
                pageable
        );

        assertEquals(6, result.getTotalElements(),
                "Expected to find all 6 rentals.");
    }

    @Test
    @DisplayName("""
            Verify findAllByUserIdAndActualReturnDateStatus returns only active rentals
            when isActive is true and userId is null. (Expected 4)
            """)
    void findAllByUserIdAndActualReturnDateStatus_ActiveOnly_ReturnsFourRentals() {
        Page<Rental> result = rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                null,
                true,
                pageable
        );

        assertEquals(4, result.getTotalElements(), "Expected 4 active rentals.");
        assertTrue(result.stream().noneMatch(r -> r.getActualReturnDate() != null),
                "All returned rentals must be active (actualReturnDate is null).");
    }

    @Test
    @DisplayName("""
            Verify findAllByUserIdAndActualReturnDateStatus returns only finished rentals
            when isActive is false and userId is null. (Expected 2)
            """)
    void findAllByUserIdAndActualReturnDateStatus_FinishedOnly_ReturnsTwoRentals() {
        Page<Rental> result = rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                null,
                false,
                pageable
        );

        // CORRECTED ASSERTION: Expected 2 finished rentals.
        assertEquals(2, result.getTotalElements(),
                "Expected 2 finished rentals.");
        assertTrue(result.stream().allMatch(r -> r.getActualReturnDate() != null),
                "All returned rentals must be finished (actualReturnDate is not null).");
    }

    @Test
    @DisplayName("""
            Verify findAllByUserIdAndActualReturnDateStatus returns only active rentals
            for a specific user.
            """)
    void findAllByUserIdAndActualReturnDateStatus_User1ActiveOnly_ReturnsTwoRentals() {
        Page<Rental> result = rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                user1.getId(),
                true,
                pageable
        );

        assertEquals(2, result.getTotalElements(),
                "Expected 2 active rentals for User 1.");
        assertTrue(result.stream().allMatch(r -> r.getUser().getId().equals(user1.getId())),
                "All returned rentals must belong to User 1.");
        assertTrue(result.stream().allMatch(r -> r.getActualReturnDate() == null),
                "All returned rentals must be active.");
    }

    @Test
    @DisplayName("findAllByUserIdAndActualReturnDateStatus should return empty page when no "
            + "rentals match")
    void findAllByUserIdAndActualReturnDateStatus_NoMatches_ShouldReturnEmptyPage() {
        Page<Rental> result = rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                999L, null, pageable);

        assertEquals(0, result.getTotalElements(), "Should return empty page");
        assertTrue(result.getContent().isEmpty(), "Content should be empty");
    }

    @Test
    @DisplayName("""
            Verify findOverdueRentals returns only rentals where returnDate <= today AND
            actualReturnDate IS NULL.
            """)
    void findOverdueRentals_ReturnsCorrectOverdueRentals() {
        List<Rental> result = rentalRepository.findOverdueRentals();

        assertEquals(2, result.size(), "Expected exactly 2 overdue rentals.");
        assertTrue(result.stream().allMatch(r -> r.getActualReturnDate() == null),
                "All overdue rentals must not have an actual return date.");
        assertTrue(result.stream().allMatch(r -> r.getReturnDate().isBefore(LocalDate.now())
                        || r.getReturnDate().isEqual(LocalDate.now())),
                "All overdue rentals must have a return date in the past or today.");
    }

    @Test
    @DisplayName("findOverdueRentals should return empty list when no overdue rentals exist")
    void findOverdueRentals_NoOverdueRentals_ShouldReturnEmptyList() {
        rentalRepository.deleteAllInBatch();

        createRental(user1, car1, LocalDate.now().plusDays(10), null);
        createRental(user2, car2, LocalDate.now().plusDays(5), null);
        entityManager.flush();

        List<Rental> result = rentalRepository.findOverdueRentals();

        assertTrue(result.isEmpty(), "Should return empty list when no overdue rentals");
    }

    @Test
    @DisplayName("findRentalById should return rental with eager-loaded user and car")
    void findRentalById_ExistingRental_ShouldReturnRentalWithRelations() {
        Rental rental = createRental(user1, car1, LocalDate.now().plusDays(5),
                null);
        entityManager.flush();
        entityManager.clear();

        Optional<Rental> result = rentalRepository.findRentalById(rental.getId());

        assertTrue(result.isPresent(), "Rental should be found");
        assertEquals(rental.getId(), result.get().getId());

        // Verify eager loading - these should not trigger additional queries
        assertEquals(user1.getId(), result.get().getUser().getId());
        assertEquals(car1.getId(), result.get().getCar().getId());
    }

    @Test
    @DisplayName("findRentalById should return empty Optional for non-existing rental")
    void findRentalById_NonExistingRental_ShouldReturnEmpty() {
        Optional<Rental> result = rentalRepository.findRentalById(999L);

        assertTrue(result.isEmpty(),
                "Should return empty Optional for non-existing rental");
    }
}
