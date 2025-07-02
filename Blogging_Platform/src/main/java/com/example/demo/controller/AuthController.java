package com.example.demo.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.example.demo.customuserdetails.CustomUserDetails;
import com.example.demo.response.AuthResponse;
import com.example.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.JwtUtils;
import com.example.demo.dto.AuthRequest;
import com.example.demo.customuserdetails.CustomUserDetailsService;

@RestController
@RequestMapping("/login")
public class AuthController {
	@Autowired
	private AuthService authService;
	 @PostMapping
	    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
		 return new ResponseEntity<AuthResponse>(authService.login(authRequest), HttpStatus.OK);
	    }
	
}
