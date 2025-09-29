package com.example.carsharingapp.dto.user;

import com.example.carsharingapp.model.User.Role;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
}
