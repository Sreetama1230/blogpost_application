package com.example.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.config.SecurityUtils;
import com.example.demo.model.User;
import com.example.demo.response.UserResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.KafkaService;
import com.example.demo.service.UserService;

@RestController
@RequestMapping("/admintool")
public class KafkaController {

	@Autowired
	public KafkaService service;
	@Autowired
	public AuthService authService;
	@Autowired
	public UserService userService;
	@GetMapping
	public  ResponseEntity<String> getPassword(){
	try {
		Long id=SecurityUtils.getCurrentUserId();
		User dbUser = userService.getbyId(id);
		UserResponse  response = UserResponse.convertUserResponse(dbUser);
		service.getLoginUserData(response);
		return new ResponseEntity<>( "Sending the username, check the console"  ,HttpStatus.OK);
	}catch(IllegalStateException illegalStateException) {
		return new ResponseEntity<>( "no user currently logged in..."  ,HttpStatus.OK);
	}

	}
}
