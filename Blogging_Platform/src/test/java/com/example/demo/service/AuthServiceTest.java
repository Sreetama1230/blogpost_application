package com.example.demo.service;

import com.example.demo.config.JwtUtils;

import com.example.demo.customuserdetails.CustomUserDetails;
import com.example.demo.customuserdetails.CustomUserDetailsService;
import com.example.demo.dto.AuthRequest;
import com.example.demo.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private  AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private Authentication authentication;



    @InjectMocks
    private  AuthService authService;

    CustomUserDetails ccustomUserDetails;
    UserDetails userDetails;
    private AuthRequest authRequest;
    private Collection<? extends GrantedAuthority> authorities;
    @BeforeEach
    void setUp(){

        authRequest=new AuthRequest();
        authRequest.setUsername("fake-username");
        authRequest.setPassword("fake-password");
        authorities =  List.of(() -> "fake-role");
        ccustomUserDetails =
                new CustomUserDetails(1L,authRequest.getUsername(),authRequest.getPassword(),authorities);


        userDetails = ccustomUserDetails;

    }
    @Test
    void testLogin_Success(){
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        )).thenReturn(authentication);

        when(((CustomUserDetails)authentication.getPrincipal())).thenReturn(ccustomUserDetails);

        when(jwtUtils.generateToken(ccustomUserDetails)).thenReturn("my-jwt-token");

      AuthResponse authResponse= authService.login(authRequest);
      assertEquals("my-jwt-token",authResponse.getToken());
        assertEquals(Optional.of(1L).get(),authResponse.getId());



    }



}
