package com.example.carsharingapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carsharingapp.dto.car.AddCarRequestDto;
import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.car.UpdateCarRequestDto;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.exception.RentalException;
import com.example.carsharingapp.mapper.CarMapper;
import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.repository.CarRepository;
import com.example.carsharingapp.service.car.CarServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {
    private static final Long CAR_ID = 1L;
    private static final String BRAND = "Toyota";
    private static final String MODEL = "Camry";
    private static final BigDecimal DAILY_FEE = new BigDecimal("50.00");
    private static final int INVENTORY = 5;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @InjectMocks
    private CarServiceImpl carService;

    private Car testCar;
    private CarDto testCarDto;
    private AddCarRequestDto addCarRequestDto;
    private UpdateCarRequestDto updateCarRequestDto;

    @BeforeEach
    void setUp() {
        testCar = new Car();
        testCar.setId(CAR_ID);
        testCar.setBrand(BRAND);
        testCar.setModel(MODEL);
        testCar.setDailyFee(DAILY_FEE);
        testCar.setInventory(INVENTORY);
        testCar.setType(Car.Type.SEDAN);

        testCarDto = new CarDto();
        testCarDto.setId(CAR_ID);
        testCarDto.setBrand(BRAND);
        testCarDto.setModel(MODEL);
        testCarDto.setDailyFee(DAILY_FEE);
        testCarDto.setInventory(INVENTORY);
        testCarDto.setType(Car.Type.SEDAN);

        addCarRequestDto = new AddCarRequestDto();
        addCarRequestDto.setBrand(BRAND);
        addCarRequestDto.setModel(MODEL);
        addCarRequestDto.setDailyFee(DAILY_FEE);
        addCarRequestDto.setInventory(INVENTORY);
        addCarRequestDto.setType(Car.Type.SEDAN);

        updateCarRequestDto = new UpdateCarRequestDto();
        updateCarRequestDto.setBrand("Honda");
        updateCarRequestDto.setModel("Accord");
        updateCarRequestDto.setDailyFee(new BigDecimal("60.00"));
        updateCarRequestDto.setInventory(10);
        updateCarRequestDto.setType(Car.Type.SEDAN);
    }

    @Test
    @DisplayName("addCar should create new car and return CarDto")
    void addCar_ValidRequest_ShouldReturnDto() {
        when(carMapper.toModel(addCarRequestDto)).thenReturn(testCar);
        when(carRepository.save(testCar)).thenReturn(testCar);
        when(carMapper.toDto(testCar)).thenReturn(testCarDto);

        CarDto result = carService.addCar(addCarRequestDto);

        assertEquals(testCarDto, result);
        verify(carMapper, times(1)).toModel(addCarRequestDto);
        verify(carRepository, times(1)).save(testCar);
        verify(carMapper, times(1)).toDto(testCar);
    }

    @Test
    @DisplayName("findAll should return page of CarDto")
    void findAll_ShouldReturnPageOfCarDto() {
        List<Car> carList = List.of(testCar);
        Page<Car> carPage = new PageImpl<>(carList, PAGEABLE, 1);

        when(carRepository.findAll(PAGEABLE)).thenReturn(carPage);
        when(carMapper.toDto(testCar)).thenReturn(testCarDto);

        Page<CarDto> result = carService.findAll(PAGEABLE);

        assertEquals(1, result.getTotalElements());
        assertEquals(testCarDto, result.getContent().get(0));
        verify(carRepository, times(1)).findAll(PAGEABLE);
        verify(carMapper, times(1)).toDto(testCar);
    }

    @Test
    @DisplayName("findAll should return empty page when no cars exist")
    void findAll_NoCars_ShouldReturnEmptyPage() {
        Page<Car> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);

        when(carRepository.findAll(PAGEABLE)).thenReturn(emptyPage);

        Page<CarDto> result = carService.findAll(PAGEABLE);

        assertEquals(0, result.getTotalElements());
        verify(carRepository, times(1)).findAll(PAGEABLE);
        verify(carMapper, never()).toDto(any(Car.class));
    }

    @Test
    @DisplayName("findById should return CarDto when car exists")
    void findById_ExistingCar_ShouldReturnDto() {
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
        when(carMapper.toDto(testCar)).thenReturn(testCarDto);

        CarDto result = carService.findById(CAR_ID);

        assertEquals(testCarDto, result);
        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carMapper, times(1)).toDto(testCar);
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException when car does not exist")
    void findById_NonExistingCar_ShouldThrowException() {
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> carService.findById(CAR_ID),
                "Should throw EntityNotFoundException when car is not found");

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carMapper, never()).toDto(any(Car.class));
    }

    @Test
    @DisplayName("updateCar should update existing car and return updated CarDto")
    void updateCar_ExistingCar_ShouldReturnUpdatedDto() {
        Car updatedCar = new Car();
        updatedCar.setId(CAR_ID);
        updatedCar.setBrand("Honda");
        updatedCar.setModel("Accord");
        updatedCar.setDailyFee(new BigDecimal("60.00"));
        updatedCar.setInventory(10);
        updatedCar.setType(Car.Type.SEDAN);

        CarDto updatedCarDto = new CarDto();
        updatedCarDto.setId(CAR_ID);
        updatedCarDto.setBrand("Honda");
        updatedCarDto.setModel("Accord");
        updatedCarDto.setDailyFee(new BigDecimal("60.00"));
        updatedCarDto.setInventory(10);
        updatedCarDto.setType(Car.Type.SEDAN);

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
        doNothing().when(carMapper).updateCarFromDto(updateCarRequestDto, testCar);
        when(carRepository.save(testCar)).thenReturn(updatedCar);
        when(carMapper.toDto(any(Car.class))).thenReturn(updatedCarDto);

        CarDto result = carService.updateCar(CAR_ID, updateCarRequestDto);

        assertEquals(updatedCarDto, result);
        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carMapper, times(1))
                .updateCarFromDto(updateCarRequestDto, testCar);
        verify(carRepository, times(1)).save(testCar);
        verify(carMapper, times(1)).toDto(updatedCar);
    }

    @Test
    @DisplayName("updateCar should throw EntityNotFoundException when car does not exist")
    void updateCar_NonExistingCar_ShouldThrowException() {
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> carService.updateCar(CAR_ID, updateCarRequestDto),
                "Should throw EntityNotFoundException when car is not found");

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carMapper, never()).updateCarFromDto(any(), any());
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    @DisplayName("delete should call repository deleteById")
    void delete_ShouldCallRepositoryDelete() {
        doNothing().when(carRepository).deleteById(CAR_ID);

        carService.delete(CAR_ID);

        verify(carRepository, times(1)).deleteById(CAR_ID);
    }

    @Test
    @DisplayName("updateInventory should increase inventory when delta is positive")
    void updateInventory_PositiveDelta_ShouldIncreaseInventory() {
        int delta = 3;

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
        when(carRepository.save(testCar)).thenReturn(testCar);

        carService.updateInventory(CAR_ID, delta);

        int expectedInventory = INVENTORY + delta;

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carRepository, times(1)).save(argThat(car ->
                car.getInventory() == expectedInventory
        ));
    }

    @Test
    @DisplayName("updateInventory should decrease inventory when delta is negative")
    void updateInventory_NegativeDelta_ShouldDecreaseInventory() {
        int delta = -2;

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
        when(carRepository.save(testCar)).thenReturn(testCar);

        int expectedInventory = INVENTORY + delta;

        carService.updateInventory(CAR_ID, delta);

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carRepository, times(1)).save(argThat(car ->
                car.getInventory() == expectedInventory
        ));
    }

    @Test
    @DisplayName("updateInventory should allow inventory to reach zero")
    void updateInventory_ToZero_ShouldSucceed() {
        int delta = -INVENTORY; // Makes inventory exactly 0

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
        when(carRepository.save(testCar)).thenReturn(testCar);

        carService.updateInventory(CAR_ID, delta);

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carRepository, times(1)).save(argThat(car ->
                car.getInventory() == 0
        ));
    }

    @Test
    @DisplayName("updateInventory should throw RentalException when resulting inventory "
            + "would be negative")
    void updateInventory_NegativeResult_ShouldThrowException() {
        int delta = -(INVENTORY + 1); // Would make inventory -1

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));

        assertThrows(RentalException.class,
                () -> carService.updateInventory(CAR_ID, delta),
                "Should throw RentalException when inventory would become negative");

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    @DisplayName("updateInventory should throw EntityNotFoundException when car does not exist")
    void updateInventory_NonExistingCar_ShouldThrowException() {
        int delta = 1;

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> carService.updateInventory(CAR_ID, delta),
                "Should throw EntityNotFoundException when car is not found");

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    @DisplayName("updateInventory should handle zero delta (no change)")
    void updateInventory_ZeroDelta_ShouldNotChangeInventory() {
        int delta = 0;

        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));
        when(carRepository.save(testCar)).thenReturn(testCar);

        carService.updateInventory(CAR_ID, delta);

        verify(carRepository, times(1)).findById(CAR_ID);
        verify(carRepository, times(1)).save(argThat(car ->
                car.getInventory() == INVENTORY // Unchanged
        ));
    }
}
