package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthRequest;
import com.example.demo.response.AuthResponse;
import com.example.demo.service.AuthService;

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
