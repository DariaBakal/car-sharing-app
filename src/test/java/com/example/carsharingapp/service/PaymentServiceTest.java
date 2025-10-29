package com.example.carsharingapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carsharingapp.dto.payment.CreatePaymentSessionRequestDto;
import com.example.carsharingapp.dto.payment.PaymentDto;
import com.example.carsharingapp.exception.AuthorityException;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.mapper.PaymentMapper;
import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.model.Payment.Type;
import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.repository.PaymentRepository;
import com.example.carsharingapp.repository.RentalRepository;
import com.example.carsharingapp.service.payment.PaymentServiceImpl;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    private static final BigDecimal DAILY_FEE = BigDecimal.TEN;
    private static final BigDecimal EXPECTED_RENTAL_AMOUNT = new BigDecimal("20.00");
    private static final BigDecimal EXPECTED_FINE_AMOUNT = new BigDecimal("30.00");
    private static final Long RENTAL_ID = 10L;
    private static final Long PAYMENT_ID = 1L;
    private static final String SESSION_ID = "cs_test_abc123";
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private StripeService stripeService;
    @Mock
    private Authentication authentication;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private User testManager;
    private Rental testRental;
    private CreatePaymentSessionRequestDto requestDto;
    private Payment pendingPayment;
    private PaymentDto expectedDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "baseUrl",
                "http://localhost:8080");

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testManager = new User();
        testManager.setId(2L);
        testManager.setFirstName("Test");
        testManager.setLastName("Manager");

        Car testCar = new Car();
        testCar.setId(1L);
        testCar.setDailyFee(BigDecimal.TEN);
        testCar.setBrand("Tesla");
        testCar.setModel("Model 3");

        testRental = new Rental();
        testRental.setId(RENTAL_ID);
        testRental.setUser(testUser);
        testRental.setCar(testCar);
        testRental.setRentalDate(LocalDate.of(2025, 10, 10));
        testRental.setReturnDate(LocalDate.of(2025, 10, 12));

        pendingPayment = new Payment();
        pendingPayment.setId(PAYMENT_ID);
        pendingPayment.setRental(testRental);
        pendingPayment.setSessionId(SESSION_ID);
        pendingPayment.setStatus(Status.PENDING);
        pendingPayment.setType(Type.PAYMENT);
        pendingPayment.setAmountToPay(EXPECTED_RENTAL_AMOUNT);

        requestDto = new CreatePaymentSessionRequestDto();
        requestDto.setRentalId(RENTAL_ID);
        requestDto.setType(Payment.Type.PAYMENT);

        expectedDto = new PaymentDto();
        lenient().when(paymentMapper.toDto(any(Payment.class))).thenReturn(expectedDto);

        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
        lenient().when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
    }

    private void setupManagerAuth() {
        Collection<GrantedAuthority> managerAuthorities =
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
        when(authentication.getPrincipal()).thenReturn(testManager);
        when(authentication.getAuthorities()).thenReturn((Collection) managerAuthorities);
    }

    @Test
    @DisplayName("findAll should return payments for the current user when not MANAGER")
    void findAll_NonManager_ShouldFilterByCurrentUser() {
        Long targetUserId = 99L;
        List<Payment> userPayments = List.of(pendingPayment);
        Page<Payment> page = new PageImpl<>(userPayments, PAGEABLE, 1);

        when(paymentRepository.findAllByRentalUserId(
                eq(testUser.getId()), eq(PAGEABLE))).thenReturn(page);
        paymentService.findAll(targetUserId, PAGEABLE, authentication);

        verify(paymentRepository, times(1)).findAllByRentalUserId(eq(
                testUser.getId()), eq(PAGEABLE));
        verify(paymentRepository, never()).findAllByRentalUserId(eq(targetUserId), eq(PAGEABLE));
    }

    @Test
    @DisplayName("findAll should return payments for specified user when MANAGER")
    void findAll_Manager_ShouldFilterBySpecifiedUser() {
        setupManagerAuth();

        Long targetUserId = 99L;
        List<Payment> userPayments = List.of(pendingPayment);
        Page<Payment> page = new PageImpl<>(userPayments, PAGEABLE, 1);

        when(paymentRepository.findAllByRentalUserId(eq(targetUserId), eq(PAGEABLE)))
                .thenReturn(page);

        paymentService.findAll(targetUserId, PAGEABLE, authentication);

        verify(paymentRepository, times(1)).findAllByRentalUserId(
                eq(targetUserId), eq(PAGEABLE));
    }

    @Test
    @DisplayName("findById should return payment for the owner user")
    void findById_OwnerUser_ShouldReturnDto() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));

        PaymentDto result = paymentService.findById(PAYMENT_ID, authentication);

        assertEquals(expectedDto, result);
        verify(paymentRepository, times(1)).findById(eq(PAYMENT_ID));
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException if payment is not found")
    void findById_NotFound_ShouldThrowException() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.findById(PAYMENT_ID, authentication),
                "Should throw EntityNotFoundException.");
    }

    @Test
    @DisplayName("findById should return payment for MANAGER even if not owner")
    void findById_Manager_ShouldReturnDto() {
        setupManagerAuth();

        User otherUser = new User();
        otherUser.setId(99L);
        testRental.setUser(otherUser);

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));

        PaymentDto result = paymentService.findById(PAYMENT_ID, authentication);

        assertEquals(expectedDto, result);
        verify(paymentRepository, times(1)).findById(eq(PAYMENT_ID));
    }

    @Test
    @DisplayName("findById should throw AuthorityException for non-owner, non-manager user")
    void findById_UnauthorizedUser_ShouldThrowAuthorityException() {
        User unauthorizedUser = new User();
        unauthorizedUser.setId(99L);
        when(authentication.getPrincipal()).thenReturn(unauthorizedUser);

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));

        assertThrows(AuthorityException.class,
                () -> paymentService.findById(PAYMENT_ID, authentication),
                "Should throw AuthorityException.");
    }

    @Test
    @DisplayName("""
            Checkout with valid request should successfully create and save a new PENDING payment
            and return PaymentDto
            """)
    public void checkout_withValidRequest_ShouldReturnPaymentDto() throws StripeException {
        Payment newPayment = new Payment();

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(SESSION_ID);
        when(mockSession.getUrl()).thenReturn("http://stripe.url");

        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(anyLong(), any(), any())).thenReturn(
                Optional.empty());
        when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(anyLong(), any(),
                any())).thenReturn(false);
        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(mockSession);
        when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);

        PaymentDto actualDto = paymentService.checkout(requestDto, authentication);

        assertEquals(expectedDto, actualDto,
                "The returned DTO should match the expected DTO.");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(stripeService, times(1)).createCheckoutSession(
                argThat(amount -> amount.compareTo(EXPECTED_RENTAL_AMOUNT) == 0),
                contains("success?session_id"),
                contains("cancel?session_id")
        );
    }

    @Test
    @DisplayName("Checkout should succeed when MANAGER creates payment for another user's rental")
    void checkout_Manager_ShouldSucceedForAnyRental() throws StripeException {
        setupManagerAuth();

        User otherUser = new User();
        otherUser.setId(99L);
        testRental.setUser(otherUser);

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(SESSION_ID);
        when(mockSession.getUrl()).thenReturn("http://stripe.url");

        when(rentalRepository.findRentalById(requestDto.getRentalId()))
                .thenReturn(Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(anyLong(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(anyLong(), any(), any()))
                .thenReturn(false);
        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(mockSession);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.checkout(requestDto, authentication);

        verify(stripeService, times(1)).createCheckoutSession(
                any(), any(), any());
    }

    @Test
    @DisplayName("Checkout should throw EntityNotFoundException if Rental is not found")
    void checkout_RentalNotFound_ShouldThrowException() throws StripeException {
        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.checkout(requestDto, authentication),
                "Should throw EntityNotFoundException when rental is missing.");

        verify(stripeService, never()).createCheckoutSession(any(), any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("AuthorityException should be thrown if user doesn't own rental and isn't MANAGER")
    void checkout_UnauthorizedUser_ShouldThrowAuthorityException() {
        User unauthorizedUser = new User();
        unauthorizedUser.setId(99L);

        when(authentication.getPrincipal()).thenReturn(unauthorizedUser);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.of(testRental));

        assertThrows(AuthorityException.class,
                () -> paymentService.checkout(requestDto, authentication),
                "Should throw AuthorityException for an unauthorized user.");

        verify(paymentRepository, never()).findByRentalIdAndTypeAndStatus(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Checkout should throw IllegalStateException if fine payment is requested before "
            + "car return")
    void checkout_FineRequestedBeforeReturn_ShouldThrowException() {
        requestDto.setType(Type.FINE);

        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.of(testRental));

        assertThrows(IllegalStateException.class,
                () -> paymentService.checkout(requestDto, authentication),
                "Should throw IllegalStateException because actual return date is null "
                        + "for a FINE payment.");

        verify(paymentRepository, never()).findByRentalIdAndTypeAndStatus(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Checkout should throw IllegalStateException if a PAID payment already exists "
            + "for this rental")
    void checkout_PaymentAlreadyPaid_ShouldThrowException() throws StripeException {
        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(anyLong(), any(), any()))
                .thenReturn(Optional.empty());

        when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(
                eq(RENTAL_ID), eq(Type.PAYMENT), eq(List.of(Status.PAID))))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> paymentService.checkout(requestDto, authentication),
                "Should throw IllegalStateException because this rental/type has already "
                        + "been paid for.");

        verify(stripeService, never()).createCheckoutSession(any(), any(), any());
    }

    @Test
    @DisplayName("Checkout should throw IllegalStateException if active PENDING payment "
            + "already exists")
    void checkout_ActivePendingPaymentExists_ShouldThrowException() throws StripeException {
        Payment existingPayment = new Payment();
        existingPayment.setSessionId("cs_active");

        Session mockSession = mock(Session.class);
        when(mockSession.getStatus()).thenReturn("open");

        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(
                eq(RENTAL_ID), eq(Type.PAYMENT), eq(Status.PENDING)))
                .thenReturn(Optional.of(existingPayment));
        lenient().when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(anyLong(), any(),
                any())).thenReturn(false);
        when(stripeService.getSession("cs_active")).thenReturn(mockSession);

        assertThrows(IllegalStateException.class,
                () -> paymentService.checkout(requestDto, authentication),
                "Should throw IllegalStateException because an active payment session "
                        + "exists.");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(stripeService, never()).createCheckoutSession(any(), any(), any());
    }

    @Test
    @DisplayName("Checkout should cancel expired PENDING payment and create a new one")
    void checkout_ExpiredPendingPaymentExists_ShouldCancelAndCreateNew() throws StripeException {
        Payment expiredPayment = new Payment();
        expiredPayment.setSessionId("cs_expired");
        expiredPayment.setStatus(Status.PENDING);
        expiredPayment.setId(99L);

        Session expiredStripeSession = mock(Session.class);
        when(expiredStripeSession.getStatus()).thenReturn("complete");

        Payment newPayment = new Payment();
        newPayment.setId(PAYMENT_ID);
        newPayment.setAmountToPay(EXPECTED_RENTAL_AMOUNT);

        Session newStripeSession = mock(Session.class);
        when(newStripeSession.getId()).thenReturn("cs_new");

        when(rentalRepository.findRentalById(requestDto.getRentalId())).thenReturn(
                Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(
                eq(RENTAL_ID), eq(Type.PAYMENT), eq(Status.PENDING)))
                .thenReturn(Optional.of(expiredPayment));
        when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(anyLong(), any(),
                any())).thenReturn(false);

        when(stripeService.getSession("cs_expired")).thenReturn(expiredStripeSession);
        lenient().when(paymentRepository.save(expiredPayment)).thenAnswer(invocation -> {
            expiredPayment.setStatus(Status.CANCELLED);
            return expiredPayment;
        });

        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(newStripeSession);
        when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);

        paymentService.checkout(requestDto, authentication);

        verify(paymentRepository, times(1)).save(expiredPayment);
        assertEquals(Status.CANCELLED, expiredPayment.getStatus(),
                "Old payment should be CANCELLED.");

        verify(stripeService, times(1)).createCheckoutSession(
                argThat(amount -> amount.compareTo(EXPECTED_RENTAL_AMOUNT) == 0),
                any(),
                any()
        );
        verify(paymentRepository).save(argThat(p -> p.getStatus() == Status.CANCELLED));
        verify(paymentRepository).save(argThat(p -> p.getStatus() == Status.PENDING));
    }

    @Test
    @DisplayName("Checkout should throw IllegalStateException if FINE requested with no "
            + "overdue days")
    void checkout_FineWithNoOverdueDays_ShouldThrowException() {
        testRental.setActualReturnDate(LocalDate.of(2025, 10, 11));
        requestDto.setType(Type.FINE);

        when(rentalRepository.findRentalById(requestDto.getRentalId()))
                .thenReturn(Optional.of(testRental));

        assertThrows(IllegalStateException.class,
                () -> paymentService.checkout(requestDto, authentication),
                "Should throw IllegalStateException when no overdue days.");
    }

    @Test
    @DisplayName("Checkout should calculate minimum 1 day for same-day rental")
    void checkout_SameDayRental_ShouldChargeMinimumOneDay() throws StripeException {
        testRental.setRentalDate(LocalDate.of(2025, 10, 10));
        testRental.setReturnDate(LocalDate.of(2025, 10, 10));

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(SESSION_ID);
        when(mockSession.getUrl()).thenReturn("http://stripe.url");

        when(rentalRepository.findRentalById(requestDto.getRentalId()))
                .thenReturn(Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(anyLong(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(anyLong(), any(), any()))
                .thenReturn(false);
        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(mockSession);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.checkout(requestDto, authentication);

        verify(stripeService).createCheckoutSession(eq(DAILY_FEE), anyString(), anyString());
    }

    @Test
    @DisplayName("handleSuccess should update payment status to PAID and send notification on "
            + "successful Stripe session")
    void handleSuccess_SuccessfulPayment_ShouldUpdateStatusAndNotify() throws StripeException {
        Session successfulStripeSession = mock(Session.class);
        when(successfulStripeSession.getPaymentStatus()).thenReturn("paid");

        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingPayment));
        when(stripeService.getSession(SESSION_ID)).thenReturn(successfulStripeSession);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleSuccess(SESSION_ID);

        verify(paymentRepository, times(1)).save(pendingPayment);
        assertEquals(Status.PAID, pendingPayment.getStatus(),
                "Payment status must be PAID.");

        verify(notificationService, times(1)).sendMessage(
                contains("SUCCESSFUL PAYMENT!")
        );
    }

    @Test
    @DisplayName("handleSuccess should throw EntityNotFoundException if payment is not found by "
            + "session ID")
    void handleSuccess_PaymentNotFound_ShouldThrowException() throws StripeException {
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.handleSuccess(SESSION_ID),
                "Should throw EntityNotFoundException if no payment matches "
                        + "the session ID.");

        verify(stripeService, never()).getSession(anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("handleSuccess should throw IllegalStateException if Stripe session is not paid")
    void handleSuccess_StripeSessionNotPaid_ShouldThrowException() throws StripeException {
        Session failedStripeSession = mock(Session.class);
        when(failedStripeSession.getPaymentStatus()).thenReturn("unpaid");

        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingPayment));
        when(stripeService.getSession(SESSION_ID)).thenReturn(failedStripeSession);

        assertThrows(IllegalStateException.class,
                () -> paymentService.handleSuccess(SESSION_ID),
                "Should throw IllegalStateException if session status is not 'paid'.");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("handleSuccess should return DTO immediately if payment is already PAID")
    void handleSuccess_AlreadyPaid_ShouldReturnDtoWithoutApiCall() throws StripeException {
        pendingPayment.setStatus(Status.PAID);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingPayment));

        PaymentDto result = paymentService.handleSuccess(SESSION_ID);

        assertEquals(expectedDto, result);
        verify(stripeService, never()).getSession(anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("handleCancel should update payment status to CANCELLED")
    void handleCancel_SuccessfulCancellation_ShouldUpdateStatusAndNotify() {
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingPayment));

        paymentService.handleCancel(SESSION_ID);

        verify(paymentRepository, times(1)).save(pendingPayment);
        assertEquals(Status.CANCELLED, pendingPayment.getStatus(),
                "Payment status must be CANCELLED.");
    }

    @Test
    @DisplayName("handleCancel should return DTO immediately if payment is already CANCELLED")
    void handleCancel_AlreadyCancelled_ShouldReturnDtoImmediately() {
        pendingPayment.setStatus(Status.CANCELLED);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingPayment));

        PaymentDto result = paymentService.handleCancel(SESSION_ID);

        assertEquals(expectedDto, result);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("handleCancel should throw IllegalStateException if payment is already PAID")
    void handleCancel_PaymentAlreadyPaid_ShouldThrowException() {
        pendingPayment.setStatus(Status.PAID);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingPayment));

        assertThrows(IllegalStateException.class,
                () -> paymentService.handleCancel(SESSION_ID),
                "Should throw IllegalStateException if attempting to cancel a "
                        + "PAID payment.");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("handleCancel should throw EntityNotFoundException if payment is not found by "
            + "session ID")
    void handleCancel_PaymentNotFound_ShouldThrowException() {
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.handleCancel(SESSION_ID),
                "Should throw EntityNotFoundException if no payment matches the "
                        + "session ID.");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("renewPaymentSession should throw EntityNotFoundException if payment is not found")
    void renewPaymentSession_NotFound_ShouldThrowException() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.renewPaymentSession(PAYMENT_ID, authentication),
                "Should throw EntityNotFoundException.");
    }

    @Test
    @DisplayName("renewPaymentSession should throw AuthorityException for unauthorized user")
    void renewPaymentSession_UnauthorizedUser_ShouldThrowAuthorityException() {
        User unauthorizedUser = new User();
        unauthorizedUser.setId(99L);
        when(authentication.getPrincipal()).thenReturn(unauthorizedUser);

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));

        assertThrows(AuthorityException.class,
                () -> paymentService.renewPaymentSession(PAYMENT_ID, authentication),
                "Should throw AuthorityException.");
    }

    @Test
    @DisplayName("renewPaymentSession should succeed when MANAGER renews another user's payment")
    void renewPaymentSession_Manager_ShouldSucceedForAnyPayment() throws StripeException {
        setupManagerAuth(); // ← Use here

        User otherUser = new User();
        otherUser.setId(99L);
        testRental.setUser(otherUser);
        pendingPayment.setStatus(Status.CANCELLED);
        Session newSession = mock(Session.class);
        when(newSession.getId()).thenReturn("cs_new_session");
        when(newSession.getUrl()).thenReturn("http://new.url");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));
        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(newSession);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.renewPaymentSession(PAYMENT_ID, authentication);

        verify(stripeService, times(1)).createCheckoutSession(
                any(), any(), any());
    }

    @Test
    @DisplayName("renewPaymentSession should throw IllegalStateException if payment is PAID")
    void renewPaymentSession_PaidPayment_ShouldThrowException() {
        pendingPayment.setStatus(Status.PAID);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));

        assertThrows(IllegalStateException.class,
                () -> paymentService.renewPaymentSession(PAYMENT_ID, authentication),
                "Should throw IllegalStateException.");
    }

    @Test
    @DisplayName("renewPaymentSession should throw IllegalStateException if PENDING session is "
            + "still open")
    void renewPaymentSession_ActiveSession_ShouldThrowException() throws StripeException {
        Session mockSession = mock(Session.class);
        when(mockSession.getStatus()).thenReturn("open");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));
        when(stripeService.getSession(SESSION_ID)).thenReturn(mockSession);

        assertThrows(IllegalStateException.class,
                () -> paymentService.renewPaymentSession(PAYMENT_ID, authentication),
                "Should throw IllegalStateException.");
    }

    @Test
    @DisplayName("renewPaymentSession should create new session for CANCELLED payment")
    void renewPaymentSession_CancelledPayment_ShouldCreateNewSession() throws StripeException {
        pendingPayment.setStatus(Status.CANCELLED);

        Session newSession = mock(Session.class);
        when(newSession.getId()).thenReturn("cs_new_session");
        when(newSession.getUrl()).thenReturn("http://new.url");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment));
        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(newSession);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.renewPaymentSession(PAYMENT_ID, authentication);

        verify(stripeService, never()).getSession(anyString());
        verify(stripeService, times(1)).createCheckoutSession(
                any(), any(), any());
    }

    @Test
    @DisplayName("Checkout should calculate FINE correctly with OVERDUE_MULTIPLIER")
    void checkout_FinePayment_ShouldCalculateCorrectAmount() throws StripeException {
        testRental.setActualReturnDate(LocalDate.of(2025, 10, 14));
        requestDto.setType(Type.FINE);

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(SESSION_ID);
        when(mockSession.getUrl()).thenReturn("http://stripe.url");

        when(rentalRepository.findRentalById(requestDto.getRentalId()))
                .thenReturn(Optional.of(testRental));
        when(paymentRepository.findByRentalIdAndTypeAndStatus(anyLong(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.existsByRentalIdAndTypeAndStatusIn(anyLong(), any(), any()))
                .thenReturn(false);
        when(stripeService.createCheckoutSession(any(), any(), any())).thenReturn(mockSession);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.checkout(requestDto, authentication);

        verify(stripeService, times(1)).createCheckoutSession(
                argThat(amount -> amount.compareTo(EXPECTED_FINE_AMOUNT) == 0),
                anyString(),
                anyString()
        );
    }
}
