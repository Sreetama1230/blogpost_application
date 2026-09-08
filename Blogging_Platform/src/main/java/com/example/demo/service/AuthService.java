package com.example.demo.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.demo.config.JwtUtils;
import com.example.demo.customuserdetails.CustomUserDetails;
import com.example.demo.customuserdetails.CustomUserDetailsService;
import com.example.demo.dto.AuthRequest;
import com.example.demo.kafkaservice.KafkaService;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.UserResponse;


@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final KafkaService kafkaService;


    public AuthService(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                       CustomUserDetailsService customUserDetailsService,KafkaService kafkaService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.kafkaService = kafkaService;
       
    }


    public AuthResponse login(AuthRequest authRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        // 1. Get user details from authentication
        CustomUserDetails customUserDetails1 = (CustomUserDetails) auth.getPrincipal();

        // 2. Manually set SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 3. Return success response (JWT or session or plb.title ain user data)
        String token = jwtUtils.generateToken(customUserDetails1);
        AuthResponse authResponse = new AuthResponse(customUserDetails1.getId(), token);


        String roles = customUserDetails1.getAuthorities().toString();
        authResponse.setRole(roles);
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(authRequest.getUsername());
        // publishing the event the admin tool topic
        kafkaService.getLoginUserData(userResponse);
        return  authResponse;

    }


}
