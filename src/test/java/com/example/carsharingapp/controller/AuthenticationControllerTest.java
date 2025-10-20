package com.example.carsharingapp.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carsharingapp.dto.user.UserLoginRequestDto;
import com.example.carsharingapp.dto.user.UserRegistrationRequestDto;
import com.example.carsharingapp.model.User;
import com.example.carsharingapp.repository.CarRepository;
import com.example.carsharingapp.repository.PaymentRepository;
import com.example.carsharingapp.repository.RentalRepository;
import com.example.carsharingapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class AuthenticationControllerTest {
    protected static MockMvc mockMvc;

    private static final String TEST_USER_EMAIL = "test@test.com";
    private static final String TEST_USER_PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void beforeAll(WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @BeforeEach
    void setupTestData() {
        paymentRepository.deleteAllInBatch();
        rentalRepository.deleteAllInBatch();
        carRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        String hashedPassword = passwordEncoder.encode(TEST_USER_PASSWORD);

        User user = new User()
                .setEmail(TEST_USER_EMAIL)
                .setPassword(hashedPassword)
                .setFirstName("Test")
                .setLastName("User")
                .setRole(User.Role.CUSTOMER)
                .setDeleted(false);

        userRepository.save(user);
    }

    @WithAnonymousUser
    @Test
    @DisplayName("""
            Verify register method for anonymous user with valid UserRegistrationRequestDto
            should return 200 OK and UserRegistrationResponseDto
            """)
    public void register_WithValidData_ShouldReturn200AndDto() throws Exception {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto()
                .setEmail("new.user@example.com")
                .setPassword("SecurePass123!")
                .setRepeatPassword("SecurePass123!")
                .setFirstName("New")
                .setLastName("User");

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(requestDto.getEmail()));
    }

    @WithAnonymousUser
    @Test
    @DisplayName("""
            Verify register method with mismatched password should return 400 Bad Request
            """)
    public void register_WithMismatchedPasswords_ShouldReturn400() throws Exception {
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto()
                .setEmail("bad.pass@example.com")
                .setPassword("Pass123")
                .setRepeatPassword("WrongPass")
                .setFirstName("Bad")
                .setLastName("User");

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @WithAnonymousUser
    @Test
    @DisplayName("""
            Verify login method with valid credentials for pre-loaded user
            should return 200 OK and UserLoginResponseDto (containing a token)
            """)
    public void login_WithValidCredentials_ShouldReturn200AndToken() throws Exception {
        UserLoginRequestDto requestDto = new UserLoginRequestDto()
                .setEmail("test@test.com")
                .setPassword("Password123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @WithAnonymousUser
    @Test
    @DisplayName("""
            Verify login method with invalid password should return 401 Unauthorized
            """)
    public void login_WithInvalidPassword_ShouldReturn401() throws Exception {
        UserLoginRequestDto requestDto = new UserLoginRequestDto()
                .setEmail("test@test.com")
                .setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }

    @WithAnonymousUser
    @Test
    @DisplayName("""
            Verify login method with non-existent user should return 401 Unauthorized
            """)
    public void login_WithNonExistentUser_ShouldReturn401() throws Exception {
        UserLoginRequestDto requestDto = new UserLoginRequestDto()
                .setEmail("nonexistent@user.com")
                .setPassword("AnyPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }
}
