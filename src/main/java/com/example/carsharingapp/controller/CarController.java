package com.example.carsharingapp.controller;

import com.example.carsharingapp.dto.car.AddCarRequestDto;
import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.car.UpdateCarRequestDto;
import com.example.carsharingapp.service.car.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Car management", description = "Endpoints for managing cars")
@RequiredArgsConstructor
@RestController
@RequestMapping("/cars")
public class CarController {
    private final CarService carService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Add a new car",
            description = "Add a new car with the provided details")
    public CarDto addCar(@RequestBody @Valid AddCarRequestDto requestDto) {
        return carService.addCar(requestDto);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all cars",
            description = "Get a paginated and sortable list of all available cars")
    public Page<CarDto> getAllCars(Pageable pageable) {
        return carService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get car by ID",
            description = "Get car's detailed information by its unique identifier")
    public CarDto getCarById(@PathVariable Long id) {
        return carService.findById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Update an existing car",
            description = "Update the details of an existing car by its unique identifier")
    public CarDto updateCar(@PathVariable Long id,
            @RequestBody @Valid UpdateCarRequestDto requestDto) {
        return carService.updateCar(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Delete car by ID",
            description = "Delete a car from the database by its unique identifier")
    public void delete(@PathVariable Long id) {
        carService.delete(id);
    }
}
