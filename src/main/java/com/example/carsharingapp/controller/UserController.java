package com.example.carsharingapp.controller;

import com.example.carsharingapp.dto.user.UpdateUserProfileRequestDto;
import com.example.carsharingapp.dto.user.UpdateUserRoleRequestDto;
import com.example.carsharingapp.dto.user.UserDto;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User management", description = "Endpoints for managing users")
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update user role",
            description = "Updates user role, available only for users with Role MANAGER")
    public UserDto updateUserRole(@PathVariable Long userId,
            @RequestBody @Valid UpdateUserRoleRequestDto requestDto) {
        return userService.updateUserRole(userId, requestDto);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile info",
            description = "Gets user profile info")
    public UserDto getMyProfileInfo(@AuthenticationPrincipal User user) {
        return userService.getMyProfileInfo(user);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user profile",
            description = "Updates user profile info")
    public UserDto updateUserProfile(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UpdateUserProfileRequestDto requestDto) {
        return userService.updateUserProfile(user, requestDto);
    }
}
