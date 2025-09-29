package com.example.carsharingapp.service;

import com.example.carsharingapp.dto.user.UpdateUserProfileRequestDto;
import com.example.carsharingapp.dto.user.UpdateUserRoleRequestDto;
import com.example.carsharingapp.dto.user.UserDto;
import com.example.carsharingapp.dto.user.UserRegistrationRequestDto;
import com.example.carsharingapp.dto.user.UserRegistrationResponseDto;
import com.example.carsharingapp.exception.RegistrationException;
import com.example.carsharingapp.model.User;

public interface UserService {
    UserRegistrationResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException;

    UserDto updateUserRole(Long userId, UpdateUserRoleRequestDto requestDto);

    UserDto getMyProfileInfo(User user);

    UserDto updateUserProfile(User user, UpdateUserProfileRequestDto requestDto);

}
