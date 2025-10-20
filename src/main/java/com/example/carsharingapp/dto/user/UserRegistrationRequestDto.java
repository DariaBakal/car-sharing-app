package com.example.carsharingapp.dto.user;

import com.example.carsharingapp.validation.Password;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
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
