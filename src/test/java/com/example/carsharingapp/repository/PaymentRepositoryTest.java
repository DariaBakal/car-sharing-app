package com.example.carsharingapp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.model.Payment.Type;
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
public class PaymentRepositoryTest {
    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Car car1;
    private Rental rental1;

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("TestFirst");
        user.setLastName("TestLast");
        user.setPassword("mockHashedPassword");
        user.setRole(User.Role.CUSTOMER);
        return entityManager.persist(user);
    }

    private Car createCar() {
        Car car = new Car();
        car.setBrand("Toyota");
        car.setModel("Camry");
        car.setType(Car.Type.SEDAN);
        car.setDailyFee(BigDecimal.valueOf(50.00));
        car.setInventory(5);
        return entityManager.persist(car);
    }

    private Rental createRental(User user, Car car) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(5));
        return entityManager.persist(rental);
    }

    private Payment createPayment(Rental rental, Status status, Type type, String sessionId) {
        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setStatus(status);
        payment.setType(type);
        payment.setSessionId(sessionId);
        payment.setSessionUrl("http://test.url/" + sessionId);
        payment.setAmountToPay(BigDecimal.valueOf(100.00));
        return entityManager.persist(payment);
    }

    @BeforeEach
    void setupAndCleanDb() {
        paymentRepository.deleteAllInBatch();

        user1 = createUser("user1@test.com");
        user2 = createUser("user2@test.com");
        car1 = createCar();

        rental1 = createRental(user1, car1);
        Rental rental2 = createRental(user1, car1);

        createPayment(rental1, Status.PAID, Type.PAYMENT, "session_1");
        createPayment(rental1, Status.PENDING, Type.FINE, "session_2");
        createPayment(rental2, Status.CANCELLED, Type.PAYMENT, "session_3");

        Rental rental3 = createRental(user2, car1);

        createPayment(rental3, Status.PENDING, Type.PAYMENT, "session_4");
        createPayment(rental3, Status.PAID, Type.FINE, "session_5");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findAllByUserId should return only user1's payments")
    void findAllByUserId_User1_ShouldReturnThreePayments() {
        Page<Payment> result = paymentRepository.findAllByRentalUserId(user1.getId(), pageable);

        assertEquals(3, result.getTotalElements(),
                "User1 should have 3 payments");
        assertTrue(result.stream().allMatch(p ->
                        p.getRental().getUser().getId().equals(user1.getId())),
                "All payments should belong to user1");
    }

    @Test
    @DisplayName("findAllByUserId should return only user2's payments")
    void findAllByUserId_User2_ShouldReturnTwoPayments() {
        Page<Payment> result = paymentRepository.findAllByRentalUserId(user2.getId(), pageable);

        assertEquals(2, result.getTotalElements(),
                "User2 should have 2 payments");
        assertTrue(result.stream().allMatch(p ->
                        p.getRental().getUser().getId().equals(user2.getId())),
                "All payments should belong to user2");
    }

    @Test
    @DisplayName("findAllByUserId should eager-load rental, user, and car")
    void findAllByUserId_ShouldEagerLoadRelations() {
        entityManager.clear();

        Page<Payment> result = paymentRepository.findAllByRentalUserId(user1.getId(), pageable);

        assertFalse(result.isEmpty(), "Should have payments");
        Payment payment = result.getContent().get(0);

        assertEquals(user1.getId(), payment.getRental().getUser().getId());
        assertEquals(car1.getId(), payment.getRental().getCar().getId());
    }

    @Test
    @DisplayName("findAllByUserId should return empty page for non-existing user")
    void findAllByUserId_NonExistingUser_ShouldReturnEmptyPage() {
        Page<Payment> result = paymentRepository.findAllByRentalUserId(999L, pageable);

        assertEquals(0, result.getTotalElements(),
                "Should return empty page for non-existing user");
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("findAll should respect pagination")
    void findAll_ShouldRespectPagination() {
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        Page<Payment> page1 = paymentRepository.findAll(firstPage);
        Page<Payment> page2 = paymentRepository.findAll(secondPage);

        assertEquals(2, page1.getContent().size(),
                "First page should have 2 payments");
        assertEquals(2, page2.getContent().size(),
                "Second page should have 2 payments");
        assertEquals(5, page1.getTotalElements(), "Total elements should be 5");

        List<Long> page1Ids = page1.getContent().stream().map(Payment::getId).toList();
        List<Long> page2Ids = page2.getContent().stream().map(Payment::getId).toList();

        assertTrue(page1Ids.stream().noneMatch(page2Ids::contains),
                "Pages should contain different payments");
    }

    @Test
    @DisplayName("existsByRentalUserIdAndStatus should return true when user has pending payment")
    void existsByRentalUserIdAndStatus_UserWithPendingPayment_ShouldReturnTrue() {
        boolean result = paymentRepository.existsByRentalUserIdAndStatus(
                user1.getId(), Status.PENDING);

        assertTrue(result, "User1 should have a PENDING payment");
    }

    @Test
    @DisplayName("existsByRentalUserIdAndStatus should return false when user has no "
            + "pending payment")
    void existsByRentalUserIdAndStatus_UserWithoutPendingPayment_ShouldReturnFalse() {
        User user3 = createUser("user3@test.com");
        Rental rental4 = createRental(user3, car1);
        createPayment(rental4, Status.PAID, Type.PAYMENT, "session_6");
        entityManager.flush();

        boolean result = paymentRepository.existsByRentalUserIdAndStatus(
                user3.getId(), Status.PENDING);

        assertFalse(result, "User3 should not have any PENDING payments");
    }

    @Test
    @DisplayName("findBySessionId should return payment with matching session ID")
    void findBySessionId_ExistingSession_ShouldReturnPayment() {
        Optional<Payment> result = paymentRepository.findBySessionId("session_1");

        assertTrue(result.isPresent(), "Should find payment with session_1");
        assertEquals("session_1", result.get().getSessionId());
        assertEquals(Status.PAID, result.get().getStatus());
    }

    @Test
    @DisplayName("findBySessionId should return empty for non-existing session")
    void findBySessionId_NonExistingSession_ShouldReturnEmpty() {
        Optional<Payment> result = paymentRepository.findBySessionId("non_existing");

        assertTrue(result.isEmpty(), "Should return empty for non-existing session");
    }

    @Test
    @DisplayName("existsByRentalIdAndTypeAndStatusIn should return true when matching "
            + "payment exists")
    void existsByRentalIdAndTypeAndStatusIn_MatchingPayment_ShouldReturnTrue() {
        boolean result = paymentRepository.existsByRentalIdAndTypeAndStatusIn(
                rental1.getId(),
                Type.PAYMENT,
                List.of(Status.PAID, Status.PENDING)
        );

        assertTrue(result, "Should find PAID PAYMENT for rental1");
    }

    @Test
    @DisplayName("existsByRentalIdAndTypeAndStatusIn should return false when no matching payment")
    void existsByRentalIdAndTypeAndStatusIn_NoMatch_ShouldReturnFalse() {
        boolean result = paymentRepository.existsByRentalIdAndTypeAndStatusIn(
                rental1.getId(),
                Type.PAYMENT,
                List.of(Status.PENDING)
        );

        assertFalse(result, "Should not find PENDING PAYMENT for rental1");
    }

    @Test
    @DisplayName("findByRentalIdAndTypeAndStatus should return matching payment")
    void findByRentalIdAndTypeAndStatus_MatchingPayment_ShouldReturnPayment() {
        Optional<Payment> result = paymentRepository.findByRentalIdAndTypeAndStatus(
                rental1.getId(),
                Type.FINE,
                Status.PENDING
        );

        assertTrue(result.isPresent(), "Should find PENDING FINE for rental1");
        assertEquals(rental1.getId(), result.get().getRental().getId());
        assertEquals(Type.FINE, result.get().getType());
        assertEquals(Status.PENDING, result.get().getStatus());
    }

    @Test
    @DisplayName("findByRentalIdAndTypeAndStatus should return empty when no match")
    void findByRentalIdAndTypeAndStatus_NoMatch_ShouldReturnEmpty() {
        Optional<Payment> result = paymentRepository.findByRentalIdAndTypeAndStatus(
                rental1.getId(),
                Type.PAYMENT,
                Status.PENDING
        );

        assertTrue(result.isEmpty(), "Should return empty when no matching payment");
    }

    @Test
    @DisplayName("findAllByStatus should return all payments with specified status")
    void findAllByStatus_PendingStatus_ShouldReturnTwoPayments() {
        List<Payment> result = paymentRepository.findAllByStatus(Status.PENDING);

        assertEquals(2, result.size(), "Should find 2 PENDING payments");
        assertTrue(result.stream().allMatch(p -> p.getStatus() == Status.PENDING),
                "All returned payments should be PENDING");
    }

    @Test
    @DisplayName("findAllByStatus should return empty list when no payments with status")
    void findAllByStatus_NoMatchingStatus_ShouldReturnEmpty() {
        paymentRepository.deleteAllInBatch();
        Rental rental = createRental(user1, car1);
        createPayment(rental, Status.PAID, Type.PAYMENT, "session_x");
        entityManager.flush();

        List<Payment> result = paymentRepository.findAllByStatus(Status.CANCELLED);

        assertTrue(result.isEmpty(), "Should return empty list for CANCELLED status");
    }
}
