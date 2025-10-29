package com.example.carsharingapp.service.payment;

import com.example.carsharingapp.dto.payment.CreatePaymentSessionRequestDto;
import com.example.carsharingapp.dto.payment.PaymentDto;
import com.example.carsharingapp.exception.AuthorityException;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.mapper.PaymentMapper;
import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.model.Payment.Type;
import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.repository.PaymentRepository;
import com.example.carsharingapp.repository.RentalRepository;
import com.example.carsharingapp.service.NotificationService;
import com.example.carsharingapp.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final double OVERDUE_MULTIPLIER = 1.5;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final StripeService stripeService;
    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;
    @Value("${app.base.url}")
    private String baseUrl;

    @Override
    public Page<PaymentDto> findAll(Long userId, Pageable pageable, Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        Long currentUserId = principal.getId();
        Long actualUserIdFilter = isManager(authentication) ? userId : currentUserId;

        return (actualUserIdFilter == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findAllByRentalUserId(actualUserIdFilter, pageable))
                .map(paymentMapper::toDto);
    }

    @Override
    public PaymentDto findById(Long id, Authentication authentication) {
        Payment payment = paymentRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find payment with id:" + id));
        validateUserAccessToRental(payment.getRental(), authentication);
        return paymentMapper.toDto(payment);
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }

    private void validateUserAccessToRental(Rental rental, Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        if (!isManager(authentication) && !rental.getUser().getId().equals(principal.getId())) {
            throw new AuthorityException(
                    "Access denied. You are not authorized to change this rental and its payment.");
        }
    }

    private void validateFineRequirements(Rental rental, Type type) {
        if (type == Type.FINE) {
            if (rental.getActualReturnDate() == null) {
                throw new IllegalStateException(
                        "Cannot create fine payment. Car must be returned first.");
            }
        }
    }

    private void handleExistingPendingPayment(Long rentalId, Type type) {
        Optional<Payment> existingPayment = paymentRepository.findByRentalIdAndTypeAndStatus(
                rentalId, type, Status.PENDING);
        if (existingPayment.isPresent()) {
            Payment pending = existingPayment.get();

            try {
                Session session = stripeService.getSession(pending.getSessionId());
                if ("open".equals(session.getStatus())) {
                    throw new IllegalStateException(
                            "An active payment session already exists for this rental");
                } else {
                    cancelPayment(pending);
                }
            } catch (StripeException e) {
                cancelPayment(pending);
            }
        }
    }

    private void validatePaymentNotAlreadyPaid(Long rentalId, Type type) {
        boolean paidPaymentExists = paymentRepository.existsByRentalIdAndTypeAndStatusIn(
                rentalId, type, List.of(Status.PAID));
        if (paidPaymentExists) {
            throw new IllegalStateException("This rental has already been paid for");
        }
    }

    private Payment createPaymentFromStripeSession(Rental rental, Type type,
            BigDecimal amountToPay) {
        String successUrl = baseUrl + "/api/payments/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/api/payments/cancel?session_id={CHECKOUT_SESSION_ID}";
        try {
            Session session = stripeService.createCheckoutSession(
                    amountToPay, successUrl, cancelUrl);
            Payment payment = new Payment();
            payment.setRental(rental);
            payment.setStatus(Status.PENDING);
            payment.setType(type);
            payment.setSessionUrl(session.getUrl());
            payment.setSessionId(session.getId());
            payment.setAmountToPay(amountToPay);
            return paymentRepository.save(payment);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe session: " + e.getMessage(), e);
        }
    }

    private Payment cancelPayment(Payment payment) {
        payment.setStatus(Status.CANCELLED);
        return paymentRepository.save(payment);
    }

    @Override
    public PaymentDto checkout(CreatePaymentSessionRequestDto requestDto,
            Authentication authentication) {
        Rental rental = rentalRepository.findRentalById(requestDto.getRentalId()).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find rental with id: " + requestDto.getRentalId()));

        validateUserAccessToRental(rental, authentication);
        validateFineRequirements(rental, requestDto.getType());
        handleExistingPendingPayment(rental.getId(), requestDto.getType());
        validatePaymentNotAlreadyPaid(rental.getId(), requestDto.getType());

        BigDecimal amountToPay = calculatePaymentAmount(rental, requestDto.getType());
        Payment savedPayment = createPaymentFromStripeSession(rental, requestDto.getType(),
                amountToPay);
        return paymentMapper.toDto(savedPayment);
    }

    @Transactional
    @Override
    public PaymentDto handleSuccess(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find payment for session id: " + sessionId));
        if (payment.getStatus() == Status.PAID) {
            return paymentMapper.toDto(payment);
        }
        try {
            Session session = stripeService.getSession(sessionId);
            if (!"paid".equals(session.getPaymentStatus())) {
                throw new IllegalStateException("Payment session exists but payment status is: "
                        + session.getPaymentStatus());
            }
            payment.setStatus(Status.PAID);
            Payment savedPayment = paymentRepository.save(payment);

            notificationService.sendMessage(buildSuccessfulPaymentNotificationMessage(
                    savedPayment.getRental(), savedPayment));

            return paymentMapper.toDto(savedPayment);

        } catch (StripeException e) {
            throw new RuntimeException(
                    "Failed to verify payment with Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentDto handleCancel(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find payment for session id: " + sessionId));

        if (payment.getStatus() == Status.PAID) {
            throw new IllegalStateException(
                    "Cannot cancel payment that has already been paid");
        }

        if (payment.getStatus() == Status.CANCELLED) {
            return paymentMapper.toDto(payment);
        }

        Payment cancelledPayment = cancelPayment(payment);
        return paymentMapper.toDto(cancelledPayment);
    }

    @Transactional
    @Override
    public PaymentDto renewPaymentSession(Long paymentId, Authentication authentication) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new EntityNotFoundException("Can't find payment with id:" + paymentId));
        validateUserAccessToRental(payment.getRental(), authentication);
        if (payment.getStatus() == Status.PAID) {
            throw new IllegalStateException("Can't renew session for paid payment");
        }
        if (payment.getStatus() == Status.PENDING) {
            try {
                Session session = stripeService.getSession(payment.getSessionId());
                if ("open".equals(session.getStatus())) {
                    throw new IllegalStateException(
                            "There is nothing to renew. Session is still active");
                }
                payment = cancelPayment(payment);
            } catch (StripeException e) {
                payment = cancelPayment(payment);
            }
        }
        String successUrl = baseUrl + "/api/payments/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/api/payments/cancel?session_id={CHECKOUT_SESSION_ID}";
        try {
            Session session = stripeService.createCheckoutSession(
                    payment.getAmountToPay(), successUrl, cancelUrl);
            payment.setStatus(Status.PENDING);
            payment.setSessionUrl(session.getUrl());
            payment.setSessionId(session.getId());
            paymentRepository.save(payment);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe session: " + e.getMessage(), e);
        }
        return paymentMapper.toDto(payment);
    }

    private BigDecimal calculatePaymentAmount(Rental rental, Payment.Type type) {
        if (type == Type.PAYMENT) {
            long days = ChronoUnit.DAYS.between(
                    rental.getRentalDate(),
                    rental.getReturnDate());
            if (days < 1) {
                days = 1;
            }
            return rental.getCar().getDailyFee().multiply(BigDecimal.valueOf(days));
        } else if (type == Type.FINE) {
            if (rental.getActualReturnDate() == null || rental.getReturnDate() == null) {
                throw new IllegalStateException("Cannot calculate fine without return dates");
            }
            long overdueDays = ChronoUnit.DAYS.between(
                    rental.getReturnDate(),
                    rental.getActualReturnDate()
            );
            if (overdueDays <= 0) {
                throw new IllegalStateException("No overdue days for fine calculation");
            }
            return rental.getCar().getDailyFee()
                    .multiply(BigDecimal.valueOf(overdueDays))
                    .multiply(BigDecimal.valueOf(OVERDUE_MULTIPLIER));
        }
        throw new IllegalArgumentException("Unknown payment type: " + type);
    }

    private String buildSuccessfulPaymentNotificationMessage(Rental rental, Payment payment) {
        String typeDetail = payment.getType() == Type.FINE ? "💰 FINE PAYMENT" :
                "💳 RENTAL PAYMENT";

        return String.format(
                "✅ **SUCCESSFUL PAYMENT!** %s\n\n"
                        + "🔑 Payment ID: `%d`\n"
                        + "💵 Amount Paid: `$%.2f`\n"
                        + "📅 Transaction Date: `%s`\n"
                        + "🔗 Session ID: `%s`\n\n"
                        + "👤 User: %s %s (ID: `%d`)\n"
                        + "⚙️ Car: %s %s (ID: `%d`)\n"
                        + "📝 Rental ID: `%d`",
                typeDetail,
                payment.getId(),
                payment.getAmountToPay(),
                LocalDate.now(),
                payment.getSessionId(),
                rental.getUser().getFirstName(), rental.getUser().getLastName(),
                rental.getUser().getId(),
                rental.getCar().getBrand(), rental.getCar().getModel(), rental.getCar().getId(),
                rental.getId()
        );
    }
}
