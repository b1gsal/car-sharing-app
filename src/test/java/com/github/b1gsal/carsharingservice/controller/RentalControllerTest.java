package com.github.b1gsal.carsharingservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.b1gsal.carsharingservice.config.CustomMySqlContainer;
import com.github.b1gsal.carsharingservice.dto.rental.RentalRequestDto;
import com.github.b1gsal.carsharingservice.dto.rental.RentalResponseDto;
import com.github.b1gsal.carsharingservice.service.notification.NotificationService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

class RentalControllerTest extends CustomMySqlContainer {

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
    @DisplayName("Create a new rental")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void rentCar_validRequest_shouldCreateRental() throws Exception {
        RentalRequestDto requestDto = new RentalRequestDto(
                LocalDate.now().plusDays(5),
                1L
        );
        MvcResult result = mockMvc.perform(post("/rentals")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        RentalResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RentalResponseDto.class
        );

        assertThat(actual).isNotNull();
        assertThat(actual.id()).isNotNull();
        assertThat(actual.carId()).isEqualTo(1L);
        assertThat(actual.userId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Return a rented car")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/rentals/add-two-rentals.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void returnCar_validRequest_shouldSetActualReturnDate() throws Exception {
        MvcResult result = mockMvc.perform(post("/rentals/10/return"))
                .andExpect(status().isOk())
                .andReturn();

        RentalResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RentalResponseDto.class
        );

        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.actualReturnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Get rental by ID")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/rentals/add-only-two-rentals.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void getRental_validId_shouldReturnRental() throws Exception {
        MvcResult result = mockMvc.perform(get("/rentals/10"))
                .andExpect(status().isOk())
                .andReturn();

        RentalResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RentalResponseDto.class
        );

        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.carId()).isEqualTo(1L);
        assertThat(actual.userId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Get active rentals list for authenticated user")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql",
            "classpath:database/cars/add-two-cars.sql",
            "classpath:database/rentals/add-only-two-rentals.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void getRentals_filterByActive_shouldReturnActiveRentals() throws Exception {
        mockMvc.perform(get("/rentals")
                        .param("is_active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10));
    }
}