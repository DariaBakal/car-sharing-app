package com.example.carsharingapp.service.car;

import com.example.carsharingapp.dto.car.AddCarRequestDto;
import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.car.UpdateCarRequestDto;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.exception.RentalException;
import com.example.carsharingapp.mapper.CarMapper;
import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    public CarDto addCar(AddCarRequestDto requestDto) {
        Car car = carMapper.toModel(requestDto);
        carRepository.save(car);
        return carMapper.toDto(car);
    }

    @Override
    public Page<CarDto> findAll(Pageable pageable) {
        return carRepository.findAll(pageable)
                .map(carMapper::toDto);
    }

    @Override
    public CarDto findById(Long id) {
        return carRepository.findById(id)
                .map(carMapper::toDto)
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find car with id: " + id));
    }

    @Override
    public CarDto updateCar(Long id, UpdateCarRequestDto requestDto) {
        Car car = carRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find car with id: " + id));
        carMapper.updateCarFromDto(requestDto, car);
        carRepository.save(car);
        return carMapper.toDto(car);
    }

    @Override
    public void delete(Long id) {
        carRepository.deleteById(id);
    }

    @Override
    public void updateInventory(Long carId, int delta) {
        Car car = carRepository.findById(carId).orElseThrow(
                () -> new EntityNotFoundException("Can't find car with id: " + carId));
        int newInventory = car.getInventory() + delta;
        if (newInventory < 0) {
            throw new RentalException(
                    "Cannot proceed. Inventory change would result in negative stock.");
        }
        car.setInventory(newInventory);
        carRepository.save(car);
    }
}
