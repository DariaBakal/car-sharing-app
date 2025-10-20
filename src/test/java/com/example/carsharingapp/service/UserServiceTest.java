package com.example.carsharingapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carsharingapp.dto.user.UpdateUserProfileRequestDto;
import com.example.carsharingapp.dto.user.UpdateUserRoleRequestDto;
import com.example.carsharingapp.dto.user.UserDto;
import com.example.carsharingapp.dto.user.UserRegistrationRequestDto;
import com.example.carsharingapp.dto.user.UserRegistrationResponseDto;
import com.example.carsharingapp.exception.EntityNotFoundException;
import com.example.carsharingapp.exception.RegistrationException;
import com.example.carsharingapp.mapper.UserMapper;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.model.User.Role;
import com.example.carsharingapp.repository.UserRepository;
import com.example.carsharingapp.service.user.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String NEW_EMAIL = "jane.test@example.com";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Test";
    private static final String PASSWORD = "StrongP@ssw0rd";
    private static final String ENCODED_PASSWORD = "{bcrypt}encoded_password_hash";
    private static final String NEW_FIRST_NAME = "Jane";
    private static final String NEW_PASSWORD = "NewStrongP@ssw0rd";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserRegistrationRequestDto registrationRequestDto;
    private UpdateUserRoleRequestDto updateUserRoleRequestDto;
    private UpdateUserProfileRequestDto updateUserProfileRequestDto;
    private UserDto expectedUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(EMAIL);
        testUser.setFirstName(FIRST_NAME);
        testUser.setLastName(LAST_NAME);
        testUser.setRole(Role.CUSTOMER);

        registrationRequestDto = new UserRegistrationRequestDto();
        registrationRequestDto.setEmail(EMAIL);
        registrationRequestDto.setFirstName(FIRST_NAME);
        registrationRequestDto.setLastName(LAST_NAME);
        registrationRequestDto.setPassword(PASSWORD);
        registrationRequestDto.setRepeatPassword(PASSWORD);

        updateUserRoleRequestDto = new UpdateUserRoleRequestDto();
        updateUserRoleRequestDto.setRole(User.Role.MANAGER);

        updateUserProfileRequestDto = new UpdateUserProfileRequestDto();
        updateUserProfileRequestDto.setEmail(NEW_EMAIL); // Assuming email change
        updateUserProfileRequestDto.setPassword(NEW_PASSWORD); // Assuming password change
        updateUserProfileRequestDto.setFirstName(NEW_FIRST_NAME);
        updateUserProfileRequestDto.setLastName(LAST_NAME);

        expectedUserDto = new UserDto();
        expectedUserDto.setId(USER_ID);
        expectedUserDto.setEmail(EMAIL);
        expectedUserDto.setFirstName(FIRST_NAME);
        expectedUserDto.setLastName(LAST_NAME);
        expectedUserDto.setRole(User.Role.MANAGER);
    }

    @Test
    @DisplayName(
            "register with valid request dto should create new user and return "
                    + "UserRegistrationResponseDto")
    void register_ValidRequest_ShouldReturnDto() {
        User userToSave = new User();
        userToSave.setEmail(EMAIL);
        userToSave.setFirstName(FIRST_NAME);
        userToSave.setLastName(LAST_NAME);

        User savedUser = new User();
        savedUser.setId(USER_ID);
        savedUser.setEmail(EMAIL);
        savedUser.setFirstName(FIRST_NAME);
        savedUser.setLastName(LAST_NAME);
        savedUser.setRole(User.Role.CUSTOMER);
        savedUser.setPassword(ENCODED_PASSWORD);

        UserRegistrationResponseDto expectedResponseDto = new UserRegistrationResponseDto();
        expectedResponseDto.setId(USER_ID);
        expectedResponseDto.setEmail(EMAIL);
        expectedResponseDto.setFirstName(FIRST_NAME);
        expectedResponseDto.setLastName(LAST_NAME);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userMapper.toModel(registrationRequestDto)).thenReturn(userToSave);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toRegistrationResponseDto(savedUser)).thenReturn(expectedResponseDto);

        UserRegistrationResponseDto actualResponseDto = userService.register(
                registrationRequestDto);

        assertEquals(expectedResponseDto, actualResponseDto);

        verify(userRepository, times(1)).save(argThat(user ->
                user.getRole() == User.Role.CUSTOMER
                        && user.getPassword().equals(ENCODED_PASSWORD)
                        && user.getEmail().equals(EMAIL)
        ));
        verify(userMapper, times(1)).toModel(registrationRequestDto);
        verify(passwordEncoder, times(1)).encode(PASSWORD);
        verify(userRepository, times(1)).existsByEmail(EMAIL);
        verify(userMapper, times(1)).toRegistrationResponseDto(savedUser);
    }

    @Test
    @DisplayName("register with existing email should throw RegistrationException")
    void register_ExistingEmail_ShouldThrowException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(RegistrationException.class,
                () -> userService.register(registrationRequestDto),
                "Should throw RegistrationException when user with the email provided"
                        + "already exist");

        verify(userRepository, times(1)).existsByEmail(EMAIL);
        verify(userMapper, never()).toModel(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole should update user role and return UserDto")
    void updateUserRole_ExistingUser_ShouldReturnUpdatedDto() {
        User updatedUser = testUser;
        updatedUser.setRole(User.Role.MANAGER);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        doAnswer(invocation -> {
            UpdateUserRoleRequestDto requestDto = invocation.getArgument(0);
            User user = invocation.getArgument(1);
            user.setRole(requestDto.getRole());
            return null;
        }).when(userMapper).updateRoleFromDto(updateUserRoleRequestDto, testUser);
        when(userRepository.save(testUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedUserDto);

        UserDto actualDto = userService.updateUserRole(USER_ID, updateUserRoleRequestDto);

        assertEquals(expectedUserDto, actualDto);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, times(1)).updateRoleFromDto(updateUserRoleRequestDto, testUser);
        verify(userRepository, times(1)).save(argThat(user ->
                user.getRole() == User.Role.MANAGER));
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    @DisplayName("updateUserRole should throw EntityNotFoundException when user does not exist")
    void updateUserRole_NonExistingUser_ShouldThrowException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updateUserRole(USER_ID, updateUserRoleRequestDto),
                "Should throw EntityNotFoundException when user is not found");

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, never()).updateRoleFromDto(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("getMyProfileInfo should return UserDto for the authenticated user")
    void getMyProfileInfo_ExistingUser_ShouldReturnDto() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(expectedUserDto);

        UserDto actualUserDto = userService.getMyProfileInfo(testUser);

        assertEquals(expectedUserDto, actualUserDto);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, times(1)).toDto(testUser);
    }

    @Test
    @DisplayName("getMyProfileInfo should throw EntityNotFoundException if user is not in DB")
    void getMyProfileInfo_NonExistingUser_ShouldThrowException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getMyProfileInfo(testUser),
                "Should throw EntityNotFoundException if the user is not found.");

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("updateUserProfile should update user profile and return UserDto")
    void updateUserProfile_ExistingUser_ShouldUpdateAndReturnDto() {
        User updatedUser = testUser;
        updatedUser.setFirstName(NEW_FIRST_NAME);

        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setId(USER_ID);
        updatedUserDto.setFirstName(NEW_FIRST_NAME);
        updatedUserDto.setLastName(LAST_NAME);
        updatedUserDto.setRole(User.Role.CUSTOMER);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        doAnswer(invocation -> {
            UpdateUserProfileRequestDto requestDto = invocation.getArgument(0);
            User user = invocation.getArgument(1);
            user.setFirstName(requestDto.getFirstName());
            return null;
        }).when(userMapper).updateUserProfileFromDto(updateUserProfileRequestDto, testUser);

        when(userRepository.save(testUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(updatedUserDto);

        UserDto resultDto = userService.updateUserProfile(testUser, updateUserProfileRequestDto);

        // 4. Assertions and Verifications
        assertEquals(updatedUserDto.getFirstName(), resultDto.getFirstName(),
                "The first name in the returned DTO should be updated.");

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, times(1)).updateUserProfileFromDto(updateUserProfileRequestDto,
                testUser);
        verify(userRepository, times(1)).save(argThat(user ->
                user.getFirstName().equals(NEW_FIRST_NAME)));
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    @DisplayName("updateUserProfile should throw EntityNotFoundException if the user is not in DB")
    void updateUserProfile_NonExistingUser_ShouldThrowException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updateUserProfile(testUser, updateUserProfileRequestDto),
                "Should throw EntityNotFoundException if the user is not found.");

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, never()).updateUserProfileFromDto(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }
}
