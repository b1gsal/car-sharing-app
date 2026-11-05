package com.github.b1gsal.carsharingservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.b1gsal.carsharingservice.config.CustomMySqlContainer;
import com.github.b1gsal.carsharingservice.dto.user.UserLoginRequestDto;
import com.github.b1gsal.carsharingservice.dto.user.UserLoginResponseDto;
import com.github.b1gsal.carsharingservice.dto.user.UserRegisterRequestDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

class AuthenticationControllerTest extends CustomMySqlContainer {

    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(@Autowired WebApplicationContext applicationContext) {
        mockMvc = webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Register a new user successfully")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void register_validRequest_shouldSaveUserAndReturnDto() throws Exception {
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto(
                "newuser@example.com",
                "Alice",
                "Smith",
                "password123",
                "password123"
        );

        MvcResult result = mockMvc.perform(post("/auth/registration")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserResponseDto.class
        );

        assertThat(actual).isNotNull();
        assertThat(actual.id()).isNotNull();
        assertThat(actual.email()).isEqualTo("newuser@example.com");
        assertThat(actual.firstName()).isEqualTo("Alice");
        assertThat(actual.lastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Login user successfully and return JWT token")
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {
            "classpath:database/rentals/clean-rentals-table.sql",
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/users/clean-users-table.sql"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void login_validCredentials_shouldReturnJwtToken() throws Exception {
        UserRegisterRequestDto registerDto = new UserRegisterRequestDto(
                "loginuser@example.com",
                "Bob",
                "User",
                "password123",
                "password123"
        );

        mockMvc.perform(post("/auth/registration")
                        .content(objectMapper.writeValueAsString(registerDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        UserLoginRequestDto loginDto = new UserLoginRequestDto(
                "loginuser@example.com",
                "password123"
        );

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .content(objectMapper.writeValueAsString(loginDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        UserLoginResponseDto responseDto = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserLoginResponseDto.class
        );

        assertThat(responseDto).isNotNull();
        assertThat(responseDto.token()).isNotBlank();
    }
}