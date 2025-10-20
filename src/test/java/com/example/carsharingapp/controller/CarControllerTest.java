package com.example.carsharingapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carsharingapp.dto.car.AddCarRequestDto;
import com.example.carsharingapp.dto.car.CarDto;
import com.example.carsharingapp.dto.car.UpdateCarRequestDto;
import com.example.carsharingapp.model.Car;
import com.example.carsharingapp.repository.CarRepository;
import com.example.carsharingapp.repository.RentalRepository;
import com.example.carsharingapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:database/car/setup-test-data.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class CarControllerTest {
    protected static MockMvc mockMvc;

    private static final String MANAGER_EMAIL = "manager@test.com";
    private static final String CUSTOMER_EMAIL = "customer@test.com";
    private static final Long TEST_CAR_ID = 1L;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CarRepository carRepository;

    private Car testCar;

    @BeforeAll
    static void beforeAll(WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @BeforeEach
    void setupTestData() {
        testCar = carRepository.findById(TEST_CAR_ID).orElseThrow(
                () -> new IllegalStateException("Test car must exist after SQL script execution"));
    }

    @WithMockUser(username = MANAGER_EMAIL, roles = {"MANAGER"})
    @Test
    @DisplayName("Verify addCarr when manager adds a new car,and returns 201 Created")
    void addCar_WithManagerRole_ShouldReturn201() throws Exception {
        AddCarRequestDto requestDto = new AddCarRequestDto()
                .setBrand("Ford")
                .setModel("Mustang")
                .setType(Car.Type.HATCHBACK)
                .setInventory(5)
                .setDailyFee(BigDecimal.valueOf(100.00));

        MvcResult result = mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brand").value("Ford"))
                .andExpect(jsonPath("$.model").value("Mustang"))
                .andReturn();

        CarDto responseDto = objectMapper.readValue(result.getResponse().getContentAsString(),
                CarDto.class);
        Optional<Car> createdCar = carRepository.findById(responseDto.getId());

        assertTrue(createdCar.isPresent());
        assertEquals(5, createdCar.get().getInventory());
    }

    @WithMockUser(username = MANAGER_EMAIL, roles = {"MANAGER"})
    @Test
    @DisplayName("Verify addCar when manager with invalid DTO returns 400 Bad Request")
    void addCar_WithInvalidDto_ShouldReturn400() throws Exception {
        AddCarRequestDto invalidDto = new AddCarRequestDto()
                .setBrand("Volvo")
                .setModel("XC90")
                .setType(Car.Type.SUV)
                .setInventory(-1)
                .setDailyFee(BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @Test
    @DisplayName("Verify addCar when Customer returns 403 Forbidden")
    void addCar_WithCustomerRole_ShouldReturn403() throws Exception {
        AddCarRequestDto requestDto = new AddCarRequestDto()
                .setBrand("Toyota")
                .setModel("Prius")
                .setType(Car.Type.SEDAN)
                .setInventory(1)
                .setDailyFee(BigDecimal.valueOf(30.00));

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @WithAnonymousUser
    @Test
    @DisplayName("Verify addCar when Anonymous user returns 401 Unauthorized")
    void addCar_WithAnonymousUser_ShouldReturn401() throws Exception {
        AddCarRequestDto requestDto = new AddCarRequestDto()
                .setBrand("Lada")
                .setModel("Niva")
                .setType(Car.Type.SUV)
                .setInventory(1)
                .setDailyFee(BigDecimal.valueOf(10.00));

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }

    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @Test
    @DisplayName("Verify getAllCars when Authenticated user returns cars and 200 OK")
    void getAllCars_WithAuthenticatedUser_ShouldReturn200AndOneCar() throws Exception {
        mockMvc.perform(get("/cars")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].brand").value(testCar.getBrand()))
                .andExpect(jsonPath("$.content[0].dailyFee").value(50.00));
    }

    @WithAnonymousUser
    @Test
    @DisplayName("Verify getAllCars when Anonymous user returns 401 Unauthorized")
    void getAllCars_WithAnonymousUser_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/cars")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @WithMockUser(username = MANAGER_EMAIL, roles = {"MANAGER"})
    @Test
    @DisplayName("Verify getCarById when Authenticated returns car and 200 OK")
    void getCarById_WithAuthenticatedUser_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_CAR_ID))
                .andExpect(jsonPath("$.brand").value("Tesla"))
                .andExpect(jsonPath("$.model").value("Model 3"));
    }

    @WithAnonymousUser
    @Test
    @DisplayName("Verify getCarById when Anonymous returns 401 Unauthorized")
    void getCarById_WithAnonymousUser_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @WithMockUser(username = MANAGER_EMAIL, roles = {"MANAGER"})
    @Test
    @DisplayName("Verify updateCar when Manager updates the car and returns 200 OK")
    void updateCar_WithManagerRole_ShouldReturn200() throws Exception {
        UpdateCarRequestDto requestDto = new UpdateCarRequestDto()
                .setBrand(testCar.getBrand())
                .setModel(testCar.getModel())
                .setType(testCar.getType())
                .setInventory(25)
                .setDailyFee(BigDecimal.valueOf(65.50));

        mockMvc.perform(patch("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory").value(25))
                .andExpect(jsonPath("$.dailyFee").value(65.50));
    }

    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @Test
    @DisplayName("Verify updateCar when Customer returns 403 Forbidden")
    void updateCar_WithCustomerRole_ShouldReturn403() throws Exception {
        UpdateCarRequestDto requestDto = new UpdateCarRequestDto()
                .setBrand(testCar.getBrand())
                .setModel(testCar.getModel())
                .setType(testCar.getType())
                .setInventory(25)
                .setDailyFee(testCar.getDailyFee());

        mockMvc.perform(patch("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @WithMockUser(username = MANAGER_EMAIL, roles = {"MANAGER"})
    @Test
    @DisplayName("Verify deleteCar when Manager deletes the car and returns 204 No Content")
    void deleteCar_WithManagerRole_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Car> deletedCar = carRepository.findById(TEST_CAR_ID);
        assertFalse(deletedCar.isPresent());
    }

    @WithMockUser(username = CUSTOMER_EMAIL, roles = {"CUSTOMER"})
    @Test
    @DisplayName("Verify deleteCar when Customer returns 403 Forbidden")
    void deleteCar_WithCustomerRole_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @WithAnonymousUser
    @Test
    @DisplayName("Verify deleteCar when Anonymous returns 401 Unauthorized")
    void deleteCar_WithAnonymousUser_ShouldReturn401() throws Exception {
        mockMvc.perform(delete("/cars/{id}", TEST_CAR_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
