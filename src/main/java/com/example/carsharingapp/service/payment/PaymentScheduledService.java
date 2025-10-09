package com.example.carsharingapp.service.payment;

import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.repository.PaymentRepository;
import com.example.carsharingapp.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentScheduledService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentScheduledService.class);
    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;

    @Scheduled(fixedRate = 60000)
    public void checkPaymentExpiration() {
        List<Payment> pendingPayments = paymentRepository.findAllByStatus(Status.PENDING);
        for (Payment payment : pendingPayments) {
            try {
                Session session = stripeService.getSession(payment.getSessionId());
                if ("expired".equals(session.getStatus())) {
                    payment.setStatus(Status.EXPIRED);
                    paymentRepository.save(payment);
                }
            } catch (StripeException e) {
                LOGGER.info("Error checking Stripe session {}: {}", payment.getSessionId(),
                        e.getMessage());
            }
        }
    }
}
