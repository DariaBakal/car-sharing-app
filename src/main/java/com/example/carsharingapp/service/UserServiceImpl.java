package com.example.carsharingapp.service;

import com.example.carsharingapp.dto.UserRegistrationRequestDto;
import com.example.carsharingapp.dto.UserRegistrationResponseDto;
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
        return userMapper.toDto(user);
    }
}
