package com.github.b1gsal.carsharingservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.github.b1gsal.carsharingservice.config.CustomMySqlContainer;
import com.github.b1gsal.carsharingservice.dto.role.RoleNameDto;
import com.github.b1gsal.carsharingservice.dto.user.UserResponseDto;
import com.github.b1gsal.carsharingservice.dto.user.UserUpdateDto;
import com.github.b1gsal.carsharingservice.model.RoleName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

class UserControllerTest extends CustomMySqlContainer {

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
    @DisplayName("Get profile info for authenticated user")
    @Sql(scripts = {
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/users/clean-users-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void getProfileInfo_validUser_shouldReturnUserProfile() throws Exception {
        UserResponseDto expected = new UserResponseDto(
                11L,
                "customer@example.com",
                "John",
                "Doe"
        );
        MvcResult result = mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        UserResponseDto actual = objectMapper.readValue(contentAsString, UserResponseDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Update profile info for current user")
    @Sql(scripts = {
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/users/clean-users-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithUserDetails("customer@example.com")
    void updateProfileInfo_validRequest_shouldUpdateAndReturnProfile() throws Exception {
        UserUpdateDto requestDto = new UserUpdateDto(
                "customer@example.com",
                "Johnny",
                "UpdatedLastName",
                "newpassword123",
                "newpassword123"
        );
        UserResponseDto expected = new UserResponseDto(
                11L,
                "customer@example.com",
                "Johnny",
                "UpdatedLastName"
        );
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(put("/users/me")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();
        UserResponseDto actual = objectMapper.readValue(contentAsString, UserResponseDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Update user role by manager")
    @Sql(scripts = {
            "classpath:database/users/clean-users-table.sql",
            "classpath:database/users/add-two-users.sql"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/users/clean-users-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void updateUserRole_asManager_shouldUpdateRole() throws Exception {
        RoleNameDto requestDto = new RoleNameDto(RoleName.ROLE_MANAGER.name());
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        mockMvc.perform(put("/users/11/role")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.roles[0].authority").value("ROLE_MANAGER"));
    }
}