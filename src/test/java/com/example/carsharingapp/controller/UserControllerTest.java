package com.example.carsharingapp.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carsharingapp.annotation.WithMockCustomUser;
import com.example.carsharingapp.dto.user.UpdateUserProfileRequestDto;
import com.example.carsharingapp.dto.user.UpdateUserRoleRequestDto;
import com.example.carsharingapp.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:database/user/setup-test-data.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class UserControllerTest {
    protected static MockMvc mockMvc;

    private static final String USER_ENDPOINT = "/users";
    private static final Long CUSTOMER_ID = 1L;
    private static final String CUSTOMER_EMAIL = "customer@test.com";
    private static final String CUSTOMER_FIRST_NAME = "John";
    private static final String CUSTOMER_LAST_NAME = "Doe";
    private static final Long MANAGER_ID = 2L;
    private static final String MANAGER_EMAIL = "manager@test.com";
    private static final String MANAGER_FIRST_NAME = "Jane";
    private static final String MANAGER_LAST_NAME = "Smith";

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Verify updateUserRole when Manager updates customer role to MANAGER")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void updateUserRole_ManagerUpdatesCustomerRole_ShouldReturnUpdatedUserDto() throws Exception {
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto()
                .setRole(User.Role.MANAGER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put(USER_ENDPOINT + "/{userId}/role", CUSTOMER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("Verify updateUserRole when Manager updates role to CUSTOMER")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void updateUserRole_ManagerUpdatesRoleToCustomer_ShouldReturnUpdatedUserDto() throws Exception {
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto()
                .setRole(User.Role.CUSTOMER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put(USER_ENDPOINT + "/{userId}/role", MANAGER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MANAGER_ID))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Verify updateUserRole when user not found returns 404 Not Found")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void updateUserRole_NonExistentUser_ShouldReturn404NotFound() throws Exception {
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto()
                .setRole(User.Role.MANAGER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put(USER_ENDPOINT + "/{userId}/role", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify updateUserRole when Customer attempts returns 403 Forbidden")
    @WithMockCustomUser()
    void updateUserRole_CustomerRole_ShouldReturn403Forbidden() throws Exception {
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto()
                .setRole(User.Role.MANAGER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put(USER_ENDPOINT + "/{userId}/role", CUSTOMER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify updateUserRole when Anonymous user returns 401 Unauthorized")
    @WithAnonymousUser
    void updateUserRole_AnonymousUser_ShouldReturn401Unauthorized() throws Exception {
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto()
                .setRole(User.Role.MANAGER);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put(USER_ENDPOINT + "/{userId}/role", CUSTOMER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify updateUserRole when invalid request body returns 400 Bad Request")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void updateUserRole_InvalidRequestBody_ShouldReturn400BadRequest() throws Exception {
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto()
                .setRole(null); // Invalid - role is required
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put(USER_ENDPOINT + "/{userId}/role", CUSTOMER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify getMyProfileInfo when Customer retrieves their profile")
    @WithMockCustomUser()
    void getMyProfileInfo_Customer_ShouldReturnUserDto() throws Exception {
        mockMvc.perform(get(USER_ENDPOINT + "/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.firstName").value(CUSTOMER_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(CUSTOMER_LAST_NAME))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Verify getMyProfileInfo when Manager retrieves their profile")
    @WithMockCustomUser(id = 2L, email = MANAGER_EMAIL, role = "MANAGER")
    void getMyProfileInfo_Manager_ShouldReturnUserDto() throws Exception {
        mockMvc.perform(get(USER_ENDPOINT + "/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MANAGER_ID))
                .andExpect(jsonPath("$.email").value(MANAGER_EMAIL))
                .andExpect(jsonPath("$.firstName").value(MANAGER_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(MANAGER_LAST_NAME))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("Verify getMyProfileInfo when Anonymous user returns 401 Unauthorized")
    @WithAnonymousUser
    void getMyProfileInfo_AnonymousUser_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get(USER_ENDPOINT + "/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify updateUserProfile when changing email to unique value")
    @WithMockCustomUser()
    void updateUserProfile_ChangeEmail_ShouldReturnUpdatedUserDto() throws Exception {
        String newEmail = "newemail@test.com"; // Unique email

        UpdateUserProfileRequestDto requestDto = new UpdateUserProfileRequestDto()
                .setEmail(newEmail)
                .setFirstName(CUSTOMER_FIRST_NAME)
                .setLastName(CUSTOMER_LAST_NAME)
                .setPassword("Password123!");
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(patch(USER_ENDPOINT + "/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.email").value(newEmail));
    }

    @Test
    @DisplayName("Verify updateUserProfile when Manager updates their profile")
    @WithMockCustomUser(id = 2L, email = MANAGER_EMAIL, role = "MANAGER")
    void updateUserProfile_Manager_ShouldReturnUpdatedUserDto() throws Exception {
        String newEmail = "newmanageremail@test.com";
        UpdateUserProfileRequestDto requestDto = new UpdateUserProfileRequestDto()
                .setEmail(newEmail)
                .setFirstName("ManagerUpdated")
                .setLastName("ProfileUpdated")
                .setPassword("StrongP@ssw0rd");
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(patch(USER_ENDPOINT + "/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MANAGER_ID))
                .andExpect(jsonPath("$.firstName").value("ManagerUpdated"))
                .andExpect(jsonPath("$.lastName").value("ProfileUpdated"));
    }

    @Test
    @DisplayName("Verify updateUserProfile when invalid password format returns 400 Bad Request")
    @WithMockCustomUser()
    void updateUserProfile_InvalidPasswordFormat_ShouldReturn400BadRequest() throws Exception {
        UpdateUserProfileRequestDto requestDto = new UpdateUserProfileRequestDto()
                .setPassword("weak"); // Invalid - too short or doesn't meet requirements
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(patch(USER_ENDPOINT + "/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify updateUserProfile when Anonymous user returns 401 Unauthorized")
    @WithAnonymousUser
    void updateUserProfile_AnonymousUser_ShouldReturn401Unauthorized() throws Exception {
        UpdateUserProfileRequestDto requestDto = new UpdateUserProfileRequestDto()
                .setFirstName("Unauthorized")
                .setLastName("Update");
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(patch(USER_ENDPOINT + "/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized());
    }
}
