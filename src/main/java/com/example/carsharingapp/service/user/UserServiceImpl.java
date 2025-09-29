package com.example.carsharingapp.service.user;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserRegistrationResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RegistrationException(
                    "This email: %s is already in use.".formatted(requestDto.getEmail()));
        }
        User user = userMapper.toModel(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);
        return userMapper.toRegistrationResponseDto(user);
    }

    @Override
    public UserDto updateUserRole(Long userId, UpdateUserRoleRequestDto requestDto) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("Can't find user with id: " + userId));
        userMapper.updateRoleFromDto(requestDto, user);
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
    public UserDto getMyProfileInfo(User user) {
        User userFromDb = userRepository.findById(user.getId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find user with id: " + user.getId()));
        return userMapper.toDto(userFromDb);
    }

    @Override
    public UserDto updateUserProfile(User user, UpdateUserProfileRequestDto requestDto) {
        User userFromDb = userRepository.findById(user.getId()).orElseThrow(
                () -> new EntityNotFoundException("Can't find user with id: " + user.getId()));
        userMapper.updateUserProfileFromDto(requestDto, userFromDb);
        userRepository.save(userFromDb);
        return userMapper.toDto(userFromDb);
    }
}
