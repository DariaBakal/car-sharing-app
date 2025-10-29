package com.example.carsharingapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carsharingapp.annotation.WithMockCustomUser;
import com.example.carsharingapp.dto.rental.AddRentalRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:database/rental/setup-test-data.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class RentalControllerTest {
    protected static MockMvc mockMvc;

    private static final String CUSTOMER_EMAIL = "customer@test.com";
    private static final String MANAGER_EMAIL = "manager@test.com";
    private static final Long TEST_CAR_ID = 1L;
    private static final Long CUSTOMER_RENTAL_ID = 1L;
    private static final Long MANAGER_RENTAL_ID = 2L;
    private static final Long CUSTOMER_USER_ID = 1L;
    private static final String RENTAL_ENDPOINT = "/rentals";

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    private AddRentalRequestDto createValidAddRentalRequestDto() {
        return new AddRentalRequestDto()
                .setCarId(TEST_CAR_ID)
                .setReturnDate(LocalDate.now().plusDays(3));
    }

    @Test
    @DisplayName("verify addRental when Authenticated user creates rental and returns 201)")
    @WithMockCustomUser()
    void addRental_WithAuthenticatedUser_ShouldReturn201Created() throws Exception {
        AddRentalRequestDto requestDto = createValidAddRentalRequestDto();
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post(RENTAL_ENDPOINT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.car.id").value(TEST_CAR_ID))
                .andExpect(jsonPath("$.user.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.returnDate")
                        .value(requestDto.getReturnDate().toString()));
    }

    @Test
    @DisplayName("Verify addRental when Unauthenticated user returns 403 Forbidden")
    void addRental_WithoutAuthentication_ShouldReturn403() throws Exception {
        AddRentalRequestDto requestDto = createValidAddRentalRequestDto();
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post(RENTAL_ENDPOINT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify getAllRentals when Customer returns page of their own active RentalDto")
    @WithMockCustomUser()
    void getAllRentals_WithCustomerRole_ShouldReturnPageOfOwnActiveRentalDto() throws Exception {
        MvcResult result = mockMvc.perform(get(RENTAL_ENDPOINT)
                        .param("isActive", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = root.get("content");

        assertEquals(CUSTOMER_RENTAL_ID, content.get(0).get("id").asLong());
        assertEquals(CUSTOMER_EMAIL, content.get(0).get("user").get("email").asText());
    }

    @Test
    @DisplayName("Verify getAllRentals when Manager requests rentals filtered by Customer ID")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void getAllRentals_WithManagerRole_ShouldAllowFilteringByUserId() throws Exception {
        MvcResult result = mockMvc.perform(get(RENTAL_ENDPOINT)
                        .param("userId", CUSTOMER_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, root.get("totalElements").asInt());
        assertEquals(CUSTOMER_RENTAL_ID, root.get("content").get(0).get("id").asLong());
        assertEquals(CUSTOMER_EMAIL, root.get("content").get(0).get("user").get("email").asText());
    }

    @Test
    @DisplayName("Verify getAllRentals when Anonymous user returns 403 Forbidden")
    @WithAnonymousUser
    void getAllRentals_WithAnonymousUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get(RENTAL_ENDPOINT)
                        .param("isActive", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify getRentalById when Customer retrieves their own Rental (ID 1)")
    @WithMockCustomUser()
    void getRentalById_OwnRental_ShouldReturnRentalDto() throws Exception {
        mockMvc.perform(get(RENTAL_ENDPOINT + "/{id}", CUSTOMER_RENTAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_RENTAL_ID))
                .andExpect(jsonPath("$.user.email").value(CUSTOMER_EMAIL));
    }

    @Test
    @DisplayName("Verify getRentalById when Customer retrieves another user's Rental (ID 2) "
            + "returns 403 Forbidden")
    @WithMockCustomUser()
    void getRentalById_AnotherUsersRental_ShouldFailAuthorization() throws Exception {
        mockMvc.perform(get(RENTAL_ENDPOINT + "/{id}", MANAGER_RENTAL_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify getRentalById when Manager retrieves any Rental (ID 1)")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void getRentalById_ManagerAccess_ShouldReturnAnyRentalDto() throws Exception {
        mockMvc.perform(get(RENTAL_ENDPOINT + "/{id}", CUSTOMER_RENTAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_RENTAL_ID))
                .andExpect(jsonPath("$.user.email").value(CUSTOMER_EMAIL));
    }

    @Test
    @DisplayName("Verify setReturnDate when Customer returns their own active Rental (ID 1)")
    @WithMockCustomUser()
    void setReturnDate_OwnRental_ShouldReturnRentalDto() throws Exception {
        mockMvc.perform(post(RENTAL_ENDPOINT + "/{id}/return", CUSTOMER_RENTAL_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_RENTAL_ID))
                .andExpect(jsonPath("$.actualReturnDate")
                        .value(LocalDate.now().toString()));
    }

    @Test
    @DisplayName("Verify setReturnDate when Customer attempts to return another user's Rental "
            + "(ID 2) returns 403 Forbidden")
    @WithMockCustomUser()
    void setReturnDate_AnotherUsersRental_ShouldFailAuthorization() throws Exception {
        mockMvc.perform(post(RENTAL_ENDPOINT + "/{id}/return", MANAGER_RENTAL_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify setReturnDate when Unauthenticated user returns 403 Forbidden")
    @WithAnonymousUser
    void setReturnDate_WithoutAuthentication_ShouldReturn403() throws Exception {
        mockMvc.perform(post(RENTAL_ENDPOINT + "/{id}/return", CUSTOMER_RENTAL_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
