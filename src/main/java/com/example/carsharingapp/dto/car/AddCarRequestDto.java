package com.example.carsharingapp.dto.car;

import com.example.carsharingapp.model.Car.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AddCarRequestDto {
    @NotBlank
    private String model;
    @NotBlank
    private String brand;
    @NotNull
    private Type type;
    @Positive
    private int inventory;
    @NotNull
    @Positive
    private BigDecimal dailyFee;
}
