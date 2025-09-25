package com.example.carsharingapp.dto;

import com.example.carsharingapp.validation.Password;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginRequestDto {
    @NotBlank
    private String email;
    @Password
    private String password;
}
