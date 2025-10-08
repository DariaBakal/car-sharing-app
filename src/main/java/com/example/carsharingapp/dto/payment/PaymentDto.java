package com.example.carsharingapp.dto.payment;

import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.model.Payment.Type;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentDto {
    private Long id;
    private Status status;
    private Type type;
    private Long rentalId;
    private String sessionUrl;
    private String sessionId;
    private BigDecimal amountToPay;
}
