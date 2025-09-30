package com.example.carsharingapp.service.car;

import com.example.carsharingapp.dto.car.AddCarRequestDto;
import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.car.UpdateCarRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {
    CarDto addCar(AddCarRequestDto requestDto);

    Page<CarDto> findAll(Pageable pageable);

    CarDto findById(Long id);

    CarDto updateCar(Long id, UpdateCarRequestDto requestDto);

    void delete(Long id);

    void updateInventory(Long carId, int delta);
}
