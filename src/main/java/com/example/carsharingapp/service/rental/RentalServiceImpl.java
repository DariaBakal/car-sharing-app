package com.example.carsharingapp.service.rental;

import com.example.carsharingapp.dto.rental.AddRentalRequestDto;
import com.example.carsharingapp.dto.rental.RentalDto;
import com.example.carsharingapp.exception.AuthorityException;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.exception.RentalException;
import com.example.carsharingapp.mapper.RentalMapper;
import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.repository.CarRepository;
import com.example.carsharingapp.repository.RentalRepository;
import com.example.carsharingapp.service.NotificationService;
import com.example.carsharingapp.service.car.CarService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final CarRepository carRepository;
    private final CarService carService;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public RentalDto addRental(AddRentalRequestDto requestDto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        Long carId = requestDto.getCarId();
        Car car = carRepository.findById(carId).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find car with id: " + carId));
        carService.updateInventory(car.getId(), -1);

        Rental rental = rentalMapper.toModel(requestDto);
        rental.setUser(user);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now());
        rentalRepository.save(rental);

        notificationService.sendMessage(buildRentalCreatedNotificationMessage(rental, car, user));
        return rentalMapper.toDto(rental);
    }

    @Override
    public Page<RentalDto> findAll(Long userId, Boolean isActive, Pageable pageable,
            Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        Long currentUserId = principal.getId();
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        Long actualUserIdFilter;
        if (isManager) {
            actualUserIdFilter = userId;
        } else {
            actualUserIdFilter = currentUserId;
        }
        return rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                        actualUserIdFilter, isActive, pageable)
                .map(rentalMapper::toDto);
    }

    @Override
    public RentalDto findById(Long id, Authentication authentication) {
        Rental rental = rentalRepository.findRentalById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find rental with id: " + id));
        User principal = (User) authentication.getPrincipal();
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (!isManager && !rental.getUser().getId().equals(principal.getId())) {
            throw new AuthorityException(
                    "Access denied. You are not authorized to view this rental record.");
        }
        return rentalMapper.toDto(rental);
    }

    @Transactional
    @Override
    public RentalDto setReturnDate(Long id, Authentication authentication) {
        Rental rental = rentalRepository.findRentalById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find rental with id: " + id));

        User principal = (User) authentication.getPrincipal();
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (!isManager && !rental.getUser().getId().equals(principal.getId())) {
            throw new AuthorityException(
                    "Access denied. You are not authorized to view and change this rental record.");
        }
        if (rental.getActualReturnDate() != null) {
            throw new RentalException("Rental with id: %s already has actual return date: %s"
                    .formatted(id, rental.getActualReturnDate()));
        }
        rental.setActualReturnDate(LocalDate.now());
        rentalRepository.save(rental);
        carService.updateInventory(rental.getCar().getId(), +1);

        notificationService.sendMessage(buildCarReturnedNotificationMessage(rental, rental.getCar(),
                rental.getUser()));

        return rentalMapper.toDto(rental);
    }

    private String buildRentalCreatedNotificationMessage(Rental rental, Car car, User user) {
        return String.format(
                "📢 **New Rental Created!** 🚗\n"
                        + "👤 User: %s %s (ID: `%d`)\n"
                        + "⚙️ Car: %s %s (ID: `%d`)\n"
                        + "🗓️ Rental Date: `%s`\n"
                        + "🔙 Expected Return: `%s`",
                user.getFirstName(), user.getLastName(), user.getId(),
                car.getBrand(), car.getModel(), car.getId(),
                rental.getRentalDate(), rental.getReturnDate()
        );
    }

    private String buildCarReturnedNotificationMessage(Rental rental, Car car, User rentalUser) {
        return String.format(
                "✅ **Car Returned Successfully!** 📦\n"
                        + "⚙️ Car: %s %s (ID: `%d`)\n"
                        + "👤 Renter: %s %s (ID: `%d`)\n"
                        + "📅 Actual Return Date: `%s`\n"
                        + "🔑 Rental ID: `%d`",
                car.getBrand(), car.getModel(), car.getId(),
                rentalUser.getFirstName(), rentalUser.getLastName(), rentalUser.getId(),
                rental.getActualReturnDate(), rental.getId()
        );
    }
}
