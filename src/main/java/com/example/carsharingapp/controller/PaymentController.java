package com.example.carsharingapp.controller;

import com.example.carsharingapp.dto.payment.CreatePaymentSessionRequestDto;
import com.example.carsharingapp.dto.payment.PaymentDto;
import com.example.carsharingapp.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment management", description = "Endpoints for managing payments")
@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get users payments",
            description = "Admin users can filter by user_id. Customers can only view their own")
    public Page<PaymentDto> getAllPayments(
            @RequestParam(required = false) Long userId,
            Pageable pageable,
            Authentication authentication) {
        return paymentService.findAll(userId, pageable, authentication);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by ID",
            description = "Admin user can get any payment, customers can only view their own.")
    public PaymentDto getPaymentById(@PathVariable Long id, Authentication authentication) {
        return paymentService.findById(id, authentication);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Initiate the payment process",
            description = "Creates a PENDING record in the DB, and gets the external sessionUrl to "
                    + "redirect the user.")
    public ResponseEntity<?> checkout(
            @RequestBody @Valid CreatePaymentSessionRequestDto requestDto,
            Authentication authentication) {
        PaymentDto paymentDto = paymentService.checkout(requestDto, authentication);
        return ResponseEntity.status(303)
                .location(URI.create(paymentDto.getSessionUrl()))
                .body(paymentDto);
    }

    @GetMapping("/success")
    @Operation(summary = "Update payment status to PAID",
            description = "The endpoint the external provider calls or redirects to upon successful"
                    + " payment, triggering the status update to PAID.")
    public String success(@RequestParam("session_id") String sessionId) {
        PaymentDto paidPayment = paymentService.handleSuccess(sessionId);
        return "Payment successful! Session ID: " + paidPayment.getSessionId();
    }

    @GetMapping("/cancel")
    @Operation(summary = "Handle canceled payment",
            description = "The external provider redirects here. Updates payment status and returns"
                    + " cancel message.")
    public String cancel(@RequestParam("session_id") String sessionId) {
        paymentService.handleCancel(sessionId);
        return "Payment was canceled. You can try again later.";
    }
}
