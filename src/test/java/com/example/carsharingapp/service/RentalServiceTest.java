package com.example.carsharingapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.rental.AddRentalRequestDto;
import com.example.carsharingapp.dto.rental.RentalDto;
import com.example.carsharingapp.dto.rental.RentalReturnDto;
import com.example.carsharingapp.dto.user.UserInfoDto;
import com.example.carsharingapp.exception.AuthorityException;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.exception.RentalException;
import com.example.carsharingapp.mapper.RentalMapper;
import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.model.Payment.Type;
import com.example.carsharingapp.model.Rental;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.repository.CarRepository;
import com.example.carsharingapp.repository.PaymentRepository;
import com.example.carsharingapp.repository.RentalRepository;
import com.example.carsharingapp.service.car.CarService;
import com.example.carsharingapp.service.payment.PaymentService;
import com.example.carsharingapp.service.rental.RentalServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
public class RentalServiceTest {
    private static final Long RENTAL_ID = 1L;
    private static final Long CAR_ID = 10L;
    private static final Long CUSTOMER_USER_ID = 100L;
    private static final Long MANAGER_USER_ID = 200L;
    private static final BigDecimal DAILY_FEE = new BigDecimal("50.00");

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private CarRepository carRepository;
    @Mock
    private CarService carService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private Authentication authentication;
    @Mock
    private Pageable pageable;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private User customerUser;
    private User managerUser;
    private Car testCar;
    private Rental testRental;

    private RentalDto testRentalDto;
    private RentalReturnDto testRentalReturnDto;
    private AddRentalRequestDto addRentalRequestDto;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(CUSTOMER_USER_ID);
        customerUser.setFirstName("John");
        customerUser.setLastName("Doe");
        customerUser.setEmail("john.doe@example.com");
        customerUser.setRole(User.Role.CUSTOMER);

        managerUser = new User();
        managerUser.setId(MANAGER_USER_ID);
        managerUser.setRole(User.Role.MANAGER);

        testCar = new Car();
        testCar.setId(CAR_ID);
        testCar.setBrand("Toyota");
        testCar.setModel("Camry");
        testCar.setDailyFee(DAILY_FEE);
        testCar.setInventory(5);
        testCar.setType(Car.Type.SEDAN);

        UserInfoDto testUserInfoDto = new UserInfoDto();
        testUserInfoDto.setId(CUSTOMER_USER_ID);
        testUserInfoDto.setEmail(customerUser.getEmail());
        testUserInfoDto.setFirstName(customerUser.getFirstName());
        testUserInfoDto.setLastName(customerUser.getLastName());

        CarDto testCarDto = new CarDto();
        testCarDto.setId(CAR_ID);
        testCarDto.setModel(testCar.getModel());
        testCarDto.setBrand(testCar.getBrand());
        testCarDto.setType(testCar.getType());
        testCarDto.setInventory(testCar.getInventory());
        testCarDto.setDailyFee(DAILY_FEE);

        testRental = new Rental();
        testRental.setId(RENTAL_ID);
        testRental.setRentalDate(LocalDate.now());
        testRental.setReturnDate(LocalDate.now().plusDays(5));
        testRental.setCar(testCar);
        testRental.setUser(customerUser);

        testRentalDto = new RentalDto();
        testRentalDto.setId(RENTAL_ID);
        testRentalDto.setRentalDate(testRental.getRentalDate());
        testRentalDto.setReturnDate(testRental.getReturnDate());
        testRentalDto.setCar(testCarDto);
        testRentalDto.setUser(testUserInfoDto);

        testRentalReturnDto = new RentalReturnDto();
        testRentalReturnDto.setId(RENTAL_ID);
        testRentalReturnDto.setRentalDate(testRental.getRentalDate());
        testRentalReturnDto.setReturnDate(testRental.getReturnDate());
        testRentalReturnDto.setActualReturnDate(LocalDate.now());
        testRentalReturnDto.setCar(testCarDto);
        testRentalReturnDto.setUser(testUserInfoDto);

        addRentalRequestDto = new AddRentalRequestDto();
        addRentalRequestDto.setCarId(CAR_ID);
        addRentalRequestDto.setReturnDate(LocalDate.now().plusDays(5));
    }

    private void mockAuthenticationAsUser(User user, String role) {
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        lenient().when(authentication.getPrincipal()).thenReturn(user);
        lenient().when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    private void mockAuthenticationAsManager(User user) {
        mockAuthenticationAsUser(user, "MANAGER");
    }

    private void mockAuthenticationAsCustomer(User user) {
        mockAuthenticationAsUser(user, "CUSTOMER");
    }

    @Test
    @DisplayName(
            "addRental should create rental, return rental dto, decrement inventory, "
                    + "and send notification "
                    + "on success")
    void addRental_ValidRequest_ShouldReturnDto() {
        mockAuthenticationAsCustomer(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(
                CUSTOMER_USER_ID, Status.PENDING)).thenReturn(false);
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.of(testCar));

        Rental newRental = new Rental();
        when(rentalMapper.toModel(addRentalRequestDto)).thenReturn(newRental);
        when(rentalRepository.save(any(Rental.class))).thenReturn(newRental);
        when(rentalMapper.toDto(newRental)).thenReturn(testRentalDto);

        RentalDto result = rentalService.addRental(addRentalRequestDto, authentication);

        assertEquals(testRentalDto, result);

        verify(carService, times(1)).updateInventory(CAR_ID, -1);
        verify(rentalRepository, times(1)).save(argThat(rental ->
                rental.getUser().equals(customerUser)
                        && rental.getCar().equals(testCar)
                        && rental.getRentalDate().isEqual(LocalDate.now())
        ));
        verify(notificationService, times(1)).sendMessage(anyString());
    }

    @Test
    @DisplayName("addRental should throw RentalException if user has a PENDING payment")
    void addRental_WithPendingPayment_ShouldThrowRentalException() {
        mockAuthenticationAsCustomer(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(
                CUSTOMER_USER_ID, Status.PENDING)).thenReturn(true);

        assertThrows(RentalException.class,
                () -> rentalService.addRental(addRentalRequestDto, authentication));

        verify(carRepository, never()).findById(anyLong());
        verify(carService, never()).updateInventory(anyLong(), anyInt());
        verify(rentalRepository, never()).save(any(Rental.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("addRental should throw EntityNotFoundException if the requested car ID is "
            + "not found")
    void addRental_WithInvalidCarId_ShouldThrowEntityNotFoundException() {
        mockAuthenticationAsCustomer(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(
                CUSTOMER_USER_ID, Status.PENDING)).thenReturn(false);
        when(carRepository.findById(CAR_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> rentalService.addRental(addRentalRequestDto, authentication));

        verify(carService, never()).updateInventory(anyLong(), anyInt());
        verify(rentalRepository, never()).save(any(Rental.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("findAll should return rentals for specified user when MANAGER")
    void findAll_ManagerRole_ShouldFilterBySpecifiedUserId() {
        mockAuthenticationAsManager(managerUser);
        Boolean isActive = true;
        Long targetUserId = CUSTOMER_USER_ID;

        List<Rental> rentalList = List.of(testRental);
        Page<Rental> mockRentalPage = new PageImpl<>(rentalList, pageable, 1);

        when(rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                eq(targetUserId),
                eq(isActive),
                eq(pageable)
        )).thenReturn(mockRentalPage);

        when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalDto);

        Page<RentalDto> actualRentalDtoPage = rentalService.findAll(
                targetUserId, isActive, pageable, authentication);

        assertEquals(1, actualRentalDtoPage.getTotalElements());
        assertEquals(testRentalDto, actualRentalDtoPage.getContent().get(0));

        verify(rentalRepository, times(1))
                .findAllByUserIdAndActualReturnDateStatus(
                        eq(targetUserId),
                        eq(isActive),
                        eq(pageable)
                );
        verify(rentalMapper, times(1)).toDto(testRental);
    }

    @Test
    @DisplayName("findAll should return all rentals when userId is null and user is MANAGER")
    void findAll_ManagerWithNullUserId_ShouldReturnAllRentals() {
        mockAuthenticationAsManager(managerUser);
        Boolean isActive = false;

        List<Rental> rentalList = List.of(testRental);
        Page<Rental> mockRentalPage = new PageImpl<>(rentalList, pageable, 1);

        when(rentalRepository.findAllByUserIdAndActualReturnDateStatus(
                isNull(),
                eq(isActive),
                eq(pageable)
        )).thenReturn(mockRentalPage);

        when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalDto);

        Page<RentalDto> actualRentalDtoPage = rentalService.findAll(
                null, isActive, pageable, authentication);

        assertEquals(1, actualRentalDtoPage.getTotalElements());
        assertEquals(testRentalDto, actualRentalDtoPage.getContent().get(0));

        verify(rentalRepository, times(1))
                .findAllByUserIdAndActualReturnDateStatus(
                        isNull(),
                        eq(isActive),
                        eq(pageable)
                );
        verify(rentalMapper, times(1)).toDto(testRental);
    }

    @Test
    @DisplayName("findById should return rental for owner customer")
    void findById_OwnerCustomer_ShouldReturnDto() {
        mockAuthenticationAsCustomer(customerUser);

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));
        when(rentalMapper.toDto(testRental)).thenReturn(testRentalDto);

        RentalDto result = rentalService.findById(RENTAL_ID, authentication);

        assertEquals(testRentalDto, result);
        verify(rentalRepository, times(1)).findRentalById(RENTAL_ID);
    }

    @Test
    @DisplayName("findById should return rental for MANAGER even if not owner")
    void findById_Manager_ShouldReturnDto() {
        mockAuthenticationAsManager(managerUser);

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));
        when(rentalMapper.toDto(testRental)).thenReturn(testRentalDto);

        RentalDto result = rentalService.findById(RENTAL_ID, authentication);

        assertEquals(testRentalDto, result);
        verify(rentalRepository, times(1)).findRentalById(RENTAL_ID);
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException if rental not found")
    void findById_NotFound_ShouldThrowException() {
        mockAuthenticationAsCustomer(customerUser);

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> rentalService.findById(RENTAL_ID, authentication));

        verify(rentalRepository, times(1)).findRentalById(RENTAL_ID);
    }

    @Test
    @DisplayName("findById should throw AuthorityException for non-owner customer")
    void findById_NonOwnerCustomer_ShouldThrowException() {
        User anotherUser = new User();
        anotherUser.setId(999L);
        mockAuthenticationAsCustomer(anotherUser);

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));

        assertThrows(AuthorityException.class,
                () -> rentalService.findById(RENTAL_ID, authentication));

        verify(rentalRepository, times(1)).findRentalById(RENTAL_ID);
    }

    @Test
    @DisplayName("setReturnDate should set actual return date and increment inventory when "
            + "returned on time")
    void setReturnDate_OnTime_ShouldUpdateAndIncrementInventory() {
        mockAuthenticationAsCustomer(customerUser);
        testRental.setReturnDate(LocalDate.now().plusDays(1));

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toReturnDto(testRental)).thenReturn(testRentalReturnDto);

        RentalReturnDto result = rentalService.setReturnDate(RENTAL_ID, authentication);

        assertEquals(testRentalReturnDto, result);
        assertEquals(LocalDate.now(), testRental.getActualReturnDate());
        verify(carService, times(1)).updateInventory(CAR_ID, 1);
        verify(notificationService, times(1)).sendMessage(
                argThat(msg -> msg.contains("Car Returned Successfully")));
        verify(paymentService, never()).checkout(any(), any());
    }

    @Test
    @DisplayName("setReturnDate should create FINE payment when returned late")
    void setReturnDate_Late_ShouldCreateFinePayment() {
        mockAuthenticationAsCustomer(customerUser);
        testRental.setReturnDate(LocalDate.now().minusDays(2));

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toReturnDto(testRental)).thenReturn(testRentalReturnDto);

        RentalReturnDto result = rentalService.setReturnDate(RENTAL_ID, authentication);

        assertEquals(testRentalReturnDto, result);
        verify(paymentService, times(1)).checkout(
                argThat(req -> req.getRentalId().equals(RENTAL_ID)
                        && req.getType() == Type.FINE),
                eq(authentication)
        );
        verify(notificationService, times(1)).sendMessage(
                argThat(msg -> msg.contains("Fine Issued")));
        verify(notificationService, times(1)).sendMessage(
                argThat(msg -> msg.contains("Car Returned Successfully")));
        verify(carService, times(1)).updateInventory(CAR_ID, 1);
    }

    @Test
    @DisplayName("setReturnDate should throw RentalException if already returned")
    void setReturnDate_AlreadyReturned_ShouldThrowException() {
        mockAuthenticationAsCustomer(customerUser);
        testRental.setActualReturnDate(LocalDate.now().minusDays(1));

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));

        assertThrows(RentalException.class,
                () -> rentalService.setReturnDate(RENTAL_ID, authentication));

        verify(rentalRepository, never()).save(any());
        verify(carService, never()).updateInventory(anyLong(), anyInt());
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("setReturnDate should throw EntityNotFoundException if rental not found")
    void setReturnDate_NotFound_ShouldThrowException() {
        mockAuthenticationAsCustomer(customerUser);

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> rentalService.setReturnDate(RENTAL_ID, authentication));

        verify(rentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("setReturnDate should throw AuthorityException for non-owner customer")
    void setReturnDate_NonOwnerCustomer_ShouldThrowException() {
        User anotherUser = new User();
        anotherUser.setId(999L);
        mockAuthenticationAsCustomer(anotherUser);

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));

        assertThrows(AuthorityException.class,
                () -> rentalService.setReturnDate(RENTAL_ID, authentication));

        verify(rentalRepository, never()).save(any());
        verify(carService, never()).updateInventory(anyLong(), anyInt());
    }

    @Test
    @DisplayName("setReturnDate should succeed when MANAGER returns rental for another user")
    void setReturnDate_Manager_ShouldSucceedForAnyRental() {
        mockAuthenticationAsManager(managerUser);
        testRental.setReturnDate(LocalDate.now().plusDays(1));

        when(rentalRepository.findRentalById(RENTAL_ID)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toReturnDto(testRental)).thenReturn(testRentalReturnDto);

        RentalReturnDto result = rentalService.setReturnDate(RENTAL_ID, authentication);

        assertEquals(testRentalReturnDto, result);
        verify(carService, times(1)).updateInventory(CAR_ID, 1);
    }
}
