package com.example.carsharingapp.dto.user;

import com.example.carsharingapp.model.User.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdateUserRoleRequestDto {
    @NotNull
    private Role role;
}
