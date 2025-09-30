package com.example.carsharingapp.dto.rental;

import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.user.UserInfoDto;
import java.time.LocalDate;
import lombok.Data;

@Data
public class RentalDto {
    private Long id;
    private LocalDate rentalDate;
    private LocalDate returnDate;
    private CarDto car;
    private UserInfoDto user;
}
