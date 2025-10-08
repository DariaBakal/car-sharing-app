package com.example.carsharingapp.service.payment;

import com.example.carsharingapp.dto.payment.CreatePaymentSessionRequestDto;
import com.example.carsharingapp.dto.payment.PaymentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface PaymentService {
    Page<PaymentDto> findAll(
            Long userId,
            Pageable pageable,
            Authentication authentication);

    PaymentDto findById(Long id, Authentication authentication);

    PaymentDto checkout(CreatePaymentSessionRequestDto requestDto, Authentication authentication);

    PaymentDto handleSuccess(String sessionId);

    PaymentDto handleCancel(String sessionId);
}
