package com.example.carsharingapp.dto.user;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserInfoDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
}
