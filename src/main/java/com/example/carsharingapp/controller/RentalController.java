package com.example.carsharingapp.controller;

import com.example.carsharingapp.dto.rental.AddRentalRequestDto;
import com.example.carsharingapp.dto.rental.RentalDto;
import com.example.carsharingapp.dto.rental.RentalReturnDto;
import com.example.carsharingapp.service.rental.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rental management", description = "Endpoints for managing users' car rentals")
@RequiredArgsConstructor
@RestController
@RequestMapping("/rentals")
public class RentalController {
    private final RentalService rentalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a new rental",
            description = "Add a new rental (decrease car inventory by 1)")
    public RentalDto addRental(@RequestBody @Valid AddRentalRequestDto requestDto,
            Authentication authentication) {
        return rentalService.addRental(requestDto, authentication);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get users rentals",
            description = "Admin users can filter by user_id. Customers can only view their own")
    public Page<RentalDto> getAllRentals(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable,
            Authentication authentication) {
        return rentalService.findAll(userId, isActive, pageable, authentication);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get rental by ID",
            description = "Get specific rental")
    public RentalDto getRentalById(@PathVariable Long id, Authentication authentication) {
        return rentalService.findById(id, authentication);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set return date",
            description = "Set actual return date (increase car inventory by 1)")
    public RentalReturnDto setReturnDate(@PathVariable Long id, Authentication authentication) {
        return rentalService.setReturnDate(id, authentication);
    }
}
