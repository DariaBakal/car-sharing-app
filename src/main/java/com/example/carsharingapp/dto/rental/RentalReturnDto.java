package com.example.carsharingapp.dto.rental;

import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.user.UserInfoDto;
import java.time.LocalDate;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RentalReturnDto {
    private Long id;
    private LocalDate rentalDate;
    private LocalDate returnDate;
    private LocalDate actualReturnDate;
    private CarDto car;
    private UserInfoDto user;
}

