package com.example.carsharingapp.dto.user;

import lombok.Data;

@Data
public class UserInfoDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
}
