package com.example.carsharingapp.dto.rental;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AddRentalRequestDto {
    @NotNull
    @FutureOrPresent
    private LocalDate returnDate;
    @NotNull
    @Positive
    private Long carId;
}
