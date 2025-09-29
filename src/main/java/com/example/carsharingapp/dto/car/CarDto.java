package com.example.carsharingapp.dto.car;

import com.example.carsharingapp.model.Car.Type;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CarDto {
    private Long id;
    private String model;
    private String brand;
    private Type type;
    private int inventory;
    private BigDecimal dailyFee;
}
