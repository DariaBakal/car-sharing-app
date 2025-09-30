package com.example.carsharingapp.service.rental;

import com.example.carsharingapp.dto.rental.AddRentalRequestDto;
import com.example.carsharingapp.dto.rental.RentalDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface RentalService {
    RentalDto addRental(AddRentalRequestDto requestDto, Authentication authentication);

    Page<RentalDto> findAll(
            Long userId,
            Boolean isActive,
            Pageable pageable,
            Authentication authentication);

    RentalDto findById(Long id, Authentication authentication);

    RentalDto setReturnDate(Long id, Authentication authentication);
}
