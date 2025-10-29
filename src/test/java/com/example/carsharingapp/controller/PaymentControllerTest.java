package com.example.carsharingapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carsharingapp.annotation.WithMockCustomUser;
import com.example.carsharingapp.dto.payment.CreatePaymentSessionRequestDto;
import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.service.StripeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:database/payment/setup-test-data.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Import(PaymentControllerTest.MockConfig.class)
public class PaymentControllerTest {
    protected static MockMvc mockMvc;

    private static final String MANAGER_EMAIL = "manager@test.com";
    private static final Long CUSTOMER_USER_ID = 1L;
    private static final Long CUSTOMER_PAYMENT_ID = 1L;
    private static final Long MANAGER_PAYMENT_ID = 2L;
    private static final Long CUSTOMER_RENTAL_ID = 1L;
    private static final Long MANAGER_RENTAL_ID = 2L;
    private static final String PAYMENT_ENDPOINT = "/payments";
    private static final String TEST_SESSION_ID = "cs_test_session_123";

    @Autowired
    private StripeService stripeService;

    static class MockConfig {

        @org.springframework.context.annotation.Bean
        StripeService stripeService() {
            return Mockito.mock(StripeService.class);
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    private CreatePaymentSessionRequestDto createValidCheckoutRequest() {
        return new CreatePaymentSessionRequestDto()
                .setRentalId(CUSTOMER_RENTAL_ID)
                .setType(Payment.Type.PAYMENT);
    }

    @Test
    @DisplayName("Verify getAllPayments when Customer returns page of their own payments")
    @WithMockCustomUser()
    void getAllPayments_WithCustomerRole_ShouldReturnPageOfOwnPayments() throws Exception {
        MvcResult result = mockMvc.perform(get(PAYMENT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = root.get("content");

        assertEquals(CUSTOMER_PAYMENT_ID, content.get(0).get("id").asLong());
    }

    @Test
    @DisplayName("Verify getAllPayments when Manager filters by Customer ID")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void getAllPayments_WithManagerRole_ShouldAllowFilteringByUserId() throws Exception {
        MvcResult result = mockMvc.perform(get(PAYMENT_ENDPOINT)
                        .param("userId", CUSTOMER_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, root.get("totalElements").asInt());
        assertEquals(CUSTOMER_PAYMENT_ID, root.get("content").get(0).get("id").asLong());
    }

    @Test
    @DisplayName("Verify getAllPayments when Manager gets all payments with userId=null")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void getAllPayments_WithManagerRoleNullUserId_ShouldReturnAllPayments() throws Exception {
        MvcResult result = mockMvc.perform(get(PAYMENT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, root.get("totalElements").asInt());
    }

    @Test
    @DisplayName("Verify getAllPayments when Anonymous user returns 403 Forbidden")
    @WithAnonymousUser
    void getAllPayments_WithAnonymousUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify getPaymentById when Customer retrieves their own payment")
    @WithMockCustomUser()
    void getPaymentById_OwnPayment_ShouldReturnPaymentDto() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/{id}", CUSTOMER_PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_PAYMENT_ID))
                .andExpect(jsonPath("$.rentalId").value(CUSTOMER_RENTAL_ID));
    }

    @Test
    @DisplayName("Verify getPaymentById when Customer attempts to view another user's payment "
            + "returns 403 Forbidden")
    @WithMockCustomUser()
    void getPaymentById_AnotherUsersPayment_ShouldFailAuthorization() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/{id}", MANAGER_PAYMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify getPaymentById when Manager retrieves any payment")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void getPaymentById_ManagerAccess_ShouldReturnAnyPayment() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/{id}", CUSTOMER_PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_PAYMENT_ID));
    }

    @Test
    @DisplayName("Verify getPaymentById when payment not found returns 404 Not Found")
    @WithMockCustomUser()
    void getPaymentById_NonExistentPayment_ShouldReturn404NotFound() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify getPaymentById when Anonymous user returns 403 Forbidden")
    @WithAnonymousUser
    void getPaymentById_WithAnonymousUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/{id}", CUSTOMER_PAYMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify checkout when authenticated user creates payment session returns 303")
    @WithMockCustomUser()
    void checkout_WithAuthenticatedUser_ShouldReturn303SeeOther() throws Exception {
        CreatePaymentSessionRequestDto requestDto = createValidCheckoutRequest();
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post(PAYMENT_ENDPOINT + "/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isSeeOther())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("PAYMENT"))
                .andExpect(jsonPath("$.rentalId").value(CUSTOMER_RENTAL_ID))
                .andExpect(jsonPath("$.sessionUrl").exists())
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    @DisplayName("Verify checkout when invalid rental ID returns 404 Not Found")
    @WithMockCustomUser()
    void checkout_WithInvalidRentalId_ShouldReturn404NotFound() throws Exception {
        CreatePaymentSessionRequestDto requestDto = new CreatePaymentSessionRequestDto()
                .setRentalId(999L)
                .setType(Payment.Type.PAYMENT);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post(PAYMENT_ENDPOINT + "/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify checkout when customer attempts to pay for another user's rental "
            + "returns 403 Forbidden")
    @WithMockCustomUser()
    void checkout_AnotherUsersRental_ShouldFailAuthorization() throws Exception {
        CreatePaymentSessionRequestDto requestDto = new CreatePaymentSessionRequestDto()
                .setRentalId(MANAGER_RENTAL_ID)
                .setType(Payment.Type.PAYMENT);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post(PAYMENT_ENDPOINT + "/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify checkout when invalid request body returns 400 Bad Request")
    @WithMockCustomUser()
    void checkout_WithInvalidRequestBody_ShouldReturn400BadRequest() throws Exception {
        CreatePaymentSessionRequestDto requestDto = new CreatePaymentSessionRequestDto()
                .setRentalId(null)
                .setType(null);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post(PAYMENT_ENDPOINT + "/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify success when valid session ID updates payment to PAID")
    void success_WithValidSessionId_ShouldReturnSuccessMessage() throws Exception {
        Session mockSession = Mockito.mock(Session.class);
        Mockito.when(mockSession.getPaymentStatus()).thenReturn("paid");

        Mockito.when(stripeService.getSession(TEST_SESSION_ID))
                .thenReturn(mockSession);

        mockMvc.perform(get(PAYMENT_ENDPOINT + "/success")
                        .param("session_id", TEST_SESSION_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify success when invalid session ID returns 404 Not Found")
    void success_WithInvalidSessionId_ShouldReturn404NotFound() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/success")
                        .param("session_id", "invalid_session"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify cancel when valid session ID updates payment to CANCELLED")
    @WithAnonymousUser
    void cancel_WithValidSessionId_ShouldReturnCancelMessage() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/cancel")
                        .param("session_id", TEST_SESSION_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify cancel when invalid session ID returns 404 Not Found")
    @WithAnonymousUser
    void cancel_WithInvalidSessionId_ShouldReturn404NotFound() throws Exception {
        mockMvc.perform(get(PAYMENT_ENDPOINT + "/cancel")
                        .param("session_id", "invalid_session"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify renewPaymentSession when customer renews their own payment")
    @WithMockCustomUser()
    void renewPaymentSession_OwnPayment_ShouldReturn303SeeOther() throws Exception {
        mockMvc.perform(post(PAYMENT_ENDPOINT + "/renew/{paymentId}", CUSTOMER_PAYMENT_ID)
                        .with(csrf()))
                .andExpect(status().isSeeOther())
                .andExpect(jsonPath("$.id").value(CUSTOMER_PAYMENT_ID))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.sessionUrl").exists());
    }

    @Test
    @DisplayName("Verify renewPaymentSession when customer attempts to renew another user's "
            + "payment returns 403 Forbidden")
    @WithMockCustomUser()
    void renewPaymentSession_AnotherUsersPayment_ShouldFailAuthorization() throws Exception {
        mockMvc.perform(post(PAYMENT_ENDPOINT + "/renew/{paymentId}", MANAGER_PAYMENT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify renewPaymentSession when payment not found returns 404 Not Found")
    @WithMockCustomUser()
    void renewPaymentSession_NonExistentPayment_ShouldReturn404NotFound() throws Exception {
        mockMvc.perform(post(PAYMENT_ENDPOINT + "/renew/{paymentId}", 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Verify renewPaymentSession when Manager renews any payment")
    @WithMockCustomUser(email = MANAGER_EMAIL, role = "MANAGER")
    void renewPaymentSession_ManagerAccess_ShouldRenewAnyPayment() throws Exception {
        Session mockExpiredSession = Mockito.mock(Session.class);
        Mockito.when(mockExpiredSession.getStatus()).thenReturn("expired");

        Session mockNewSession = Mockito.mock(Session.class);
        Mockito.when(mockNewSession.getId()).thenReturn("cs_renew_mock_123");
        Mockito.when(mockNewSession.getUrl()).thenReturn("http://mock-renew-url.com");

        Mockito.when(stripeService.getSession("cs_test_session_123"))
                .thenReturn(mockExpiredSession);

        Mockito.when(stripeService.createCheckoutSession(
                Mockito.any(BigDecimal.class),
                Mockito.anyString(),
                Mockito.anyString()
        )).thenReturn(mockNewSession);

        mockMvc.perform(post(PAYMENT_ENDPOINT + "/renew/{paymentId}", CUSTOMER_PAYMENT_ID)
                        .with(csrf()))
                .andExpect(status().isSeeOther())
                .andExpect(jsonPath("$.id").value(CUSTOMER_PAYMENT_ID))
                .andExpect(jsonPath("$.sessionId").value("cs_renew_mock_123"));
    }

    @Test
    @DisplayName("Verify renewPaymentSession when Anonymous user returns 403 Forbidden")
    @WithAnonymousUser
    void renewPaymentSession_WithAnonymousUser_ShouldReturn403() throws Exception {
        mockMvc.perform(post(PAYMENT_ENDPOINT + "/renew/{paymentId}", CUSTOMER_PAYMENT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
