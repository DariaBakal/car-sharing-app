package com.example.carsharingapp.dto.payment;

import com.example.carsharingapp.model.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreatePaymentSessionRequestDto {
    @NotNull
    private Long rentalId;
    @NotNull
    private Payment.Type type;
}
