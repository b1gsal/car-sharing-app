package com.github.b1gsal.carsharingservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.b1gsal.carsharingservice.config.CustomMySqlContainer;
import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import com.github.b1gsal.carsharingservice.model.Type;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

class CarControllerTest extends CustomMySqlContainer {

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
    @DisplayName("Get all cars")
    @Sql(scripts = {
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/cars/add-two-cars.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/clean-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getCars_ValidRequest_ShouldReturnTwoCars() throws Exception {
        mockMvc.perform(get("/cars").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].model").value("Octavia"))
                .andExpect(jsonPath("$.content[1].model").value("CX-5"));
    }

    @WithMockUser(username = "Manager", roles = {"MANAGER"})
    @Test
    @DisplayName("Create new car")
    @Sql(scripts = "classpath:database/cars/clean-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/clean-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createCar_validRequest_ShouldSaveCarToDbAndReturnCarDto() throws Exception {
        CarRequestDto carRequestDto = new CarRequestDto(
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                5
        );
        CarResponseDto expected = new CarResponseDto(
                null,
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                5
        );
        String jsonRequest = objectMapper.writeValueAsString(carRequestDto);
        MvcResult result = mockMvc.perform(
                        post("/cars")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isCreated())
                .andReturn();

        String contentAsString = result.getResponse().getContentAsString();
        CarResponseDto actual = objectMapper.readValue(contentAsString, CarResponseDto.class);
        assertThat(actual).isNotNull();
        assertThat(actual.id()).isNotNull();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Get current car by id")
    @Sql(scripts = {
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/cars/add-two-cars.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/clean-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getDetailedCarInfo_validRequest_ShouldReturnCarInfo() throws Exception {
        CarResponseDto expected = new CarResponseDto(
                1L,
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                5
        );
        MvcResult result = mockMvc.perform(get("/cars/1"))
                .andExpect(status().isOk())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        CarResponseDto actual = objectMapper.readValue(contentAsString, CarResponseDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @WithMockUser(username = "Manager", roles = {"MANAGER"})
    @Test
    @DisplayName("Delete car by id")
    @Sql(scripts = {
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/cars/add-two-cars.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/clean-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteById_validRequest_ShouldDeleteCarFromDb() throws Exception {
        mockMvc.perform(delete("/cars/2"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @WithMockUser(username = "Manager", roles = {"MANAGER"})
    @Test
    @DisplayName("Update car by id")
    @Sql(scripts = {
            "classpath:database/cars/clean-cars-table.sql",
            "classpath:database/cars/add-two-cars.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/clean-cars-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateCar_validRequest_shouldUpdateCar() throws Exception {
        CarUpdateDto carUpdateDto = new CarUpdateDto(
                "CX-3",
                "Mazda",
                Type.SUV,
                new BigDecimal("35.00"),
                2
        );
        CarResponseDto expected = new CarResponseDto(
                2L,
                "CX-3",
                "Mazda",
                Type.SUV,
                new BigDecimal("35.00"),
                2
        );
        String requestValue = objectMapper.writeValueAsString(carUpdateDto);
        MvcResult result = mockMvc.perform(put("/cars/2")
                        .content(requestValue)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        CarResponseDto actual = objectMapper.readValue(contentAsString, CarResponseDto.class);
        assertThat(actual).isEqualTo(expected);
    }
}