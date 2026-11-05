package com.github.b1gsal.carsharingservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.b1gsal.carsharingservice.config.CustomMySqlContainer;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentRequestDto;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.service.notification.NotificationService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

class PaymentControllerTest extends CustomMySqlContainer {

    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeAll
    static void beforeAll(@Autowired WebApplicationContext applicationContext) {
        mockMvc = webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Create payment session in DB")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/rentals/add-only-two-rentals.sql" // <-- Оновлено назву скрипту
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void createPayment_validRequest_shouldSavePaymentToDb() throws Exception {
        PaymentRequestDto requestDto = new PaymentRequestDto(10L, Payment.Type.PAYMENT);

        Session stripeSessionMock = Mockito.mock(Session.class);
        Mockito.when(stripeSessionMock.getId()).thenReturn("cs_test_mock_session_id");
        Mockito.when(stripeSessionMock.getUrl()).thenReturn("https://checkout.stripe.com/test_url");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(stripeSessionMock);

            MvcResult result = mockMvc.perform(post("/payments")
                            .content(objectMapper.writeValueAsString(requestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andReturn();

            PaymentResponseDto actual = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    PaymentResponseDto.class
            );

            assertThat(actual).isNotNull();
            assertThat(actual.id()).isNotNull();
            assertThat(actual.rentalId()).isEqualTo(10L);
            assertThat(actual.status()).isEqualTo(Payment.Status.PENDING);
        }
    }

    @Test
    @DisplayName("Process successful payment redirection and update DB")
    @Sql(scripts = {
            "classpath:database/payments/clean-payments-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/payments/add-two-payments.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/payments/clean-payments-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void handleSuccessPayment_validSessionId_shouldUpdateStatusToPaid() throws Exception {
        Session stripeSessionMock = Mockito.mock(Session.class);
        Mockito.when(stripeSessionMock.getPaymentStatus()).thenReturn("paid");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve("cs_test_10"))
                    .thenReturn(stripeSessionMock);

            MvcResult result = mockMvc.perform(get("/payments/success")
                            .param("session_id", "cs_test_10"))
                    .andExpect(status().isOk())
                    .andReturn();

            PaymentResponseDto actual = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    PaymentResponseDto.class
            );

            assertThat(actual.id()).isEqualTo(10L);
            assertThat(actual.status()).isEqualTo(Payment.Status.PAID);
        }
    }

    @Test
    @DisplayName("Cancel payment redirection notice")
    void handleCancelPayment_shouldReturnCancelMessage() throws Exception {
        mockMvc.perform(get("/payments/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Payment can be made later. The Stripe session is valid for 24 hours"));
    }

    @Test
    @DisplayName("Get payments list from DB for customer user")
    @Sql(scripts = {
            "classpath:database/payments/clean-payments-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/payments/add-two-payments.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/payments/clean-payments-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void getPayments_authenticatedCustomer_shouldReturnUserPayments() throws Exception {
        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[1].id").value(11));
    }

    @Test
    @DisplayName("Renew expired payment session in DB")
    @Sql(scripts = {
            "classpath:database/payments/clean-payments-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/payments/add-two-payments.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/payments/clean-payments-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void renewPayment_expiredPayment_shouldRecreateStripeSession() throws Exception {
        Session stripeSessionMock = Mockito.mock(Session.class);
        Mockito.when(stripeSessionMock.getId()).thenReturn("cs_test_renewed");
        Mockito.when(stripeSessionMock.getUrl()).thenReturn("https://checkout.stripe.com/renewed_url");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(stripeSessionMock);
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class), any()))
                    .thenReturn(stripeSessionMock);

            MvcResult result = mockMvc.perform(post("/payments/10/renew"))
                    .andExpect(status().isOk())
                    .andReturn();

            PaymentResponseDto actual = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    PaymentResponseDto.class
            );

            assertThat(actual.id()).isEqualTo(10L);
            assertThat(actual.status()).isEqualTo(Payment.Status.PENDING);
            assertThat(actual.sessionId()).isEqualTo("cs_test_renewed");
        }
    }
}