package com.example.carsharingapp.mapper;

import com.example.carsharingapp.config.MapperConfig;
import com.example.carsharingapp.dto.user.UpdateUserProfileRequestDto;
import com.example.carsharingapp.dto.user.UpdateUserRoleRequestDto;
import com.example.carsharingapp.dto.user.UserDto;
import com.example.carsharingapp.dto.user.UserRegistrationRequestDto;
import com.example.carsharingapp.dto.user.UserRegistrationResponseDto;
import com.example.carsharingapp.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    User toModel(UserRegistrationRequestDto requestDto);

    UserDto toDto(User user);

    UserRegistrationResponseDto toRegistrationResponseDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateRoleFromDto(UpdateUserRoleRequestDto requestDto, @MappingTarget User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateUserProfileFromDto(UpdateUserProfileRequestDto requestDto, @MappingTarget User user);
}
