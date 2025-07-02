package com.example.demo.service;

import com.example.demo.config.JwtUtils;
import com.example.demo.customuserdetails.CustomUserDetails;
import com.example.demo.customuserdetails.CustomUserDetailsService;
import com.example.demo.dto.AuthRequest;
import com.example.demo.response.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthService(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                       CustomUserDetailsService customUserDetails) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.customUserDetailsService = customUserDetails;
    }


    public AuthResponse login(AuthRequest authRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        // 1. Get user details from authentication
        CustomUserDetails customUserDetails1 = (CustomUserDetails) auth.getPrincipal();

        // 2. Manually set SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 3. Return success response (JWT or session or plain user data)
        String token = jwtUtils.generateToken(customUserDetails1);
        AuthResponse authResponse = new AuthResponse(customUserDetails1.getId(), token);


        String roles = customUserDetails1.getAuthorities().toString();
        authResponse.setRole(roles);
        return  authResponse;

    }


}
