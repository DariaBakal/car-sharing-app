package com.example.carsharingapp.dto;

import com.example.carsharingapp.validation.Password;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegistrationRequestDto {
    @NotBlank
    private String email;
    @Password
    private String password;
    @NotBlank
    private String repeatPassword;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
}
