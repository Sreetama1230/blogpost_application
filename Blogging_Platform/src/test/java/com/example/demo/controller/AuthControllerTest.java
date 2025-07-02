package com.example.demo.controller;


import com.example.demo.dto.AuthRequest;
import com.example.demo.response.AuthResponse;
import com.example.demo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebMvc
public class AuthControllerTest {
    @InjectMocks
    private AuthController authController;
    @Mock
    private AuthService authService;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp(){
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }
    @Test
    void testLogin() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("fake-username");
        authRequest.setPassword("fake-password");

        AuthResponse authResponse = new AuthResponse(1L,"fake-jwt-token");
        authResponse.setRole("ROLE_FAKE");
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authResponse.getId()))
                .andExpect(jsonPath("$.token").value(authResponse.getToken()));

        verify(authService).login(any(AuthRequest.class));


    }
}
