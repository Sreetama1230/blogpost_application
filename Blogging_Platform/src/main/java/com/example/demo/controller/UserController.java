package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostDetailsResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService s;
	
	Logger logger = LoggerFactory.getLogger(UserController.class);


	
	@PostMapping("/register")
	public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserDTO u) throws JsonProcessingException {
		User newUser = s.createUser(u);
		return new ResponseEntity<UserResponse>(UserResponse.convertUserResponse(newUser), HttpStatus.CREATED);
	}

	@GetMapping("/{userId}/post")
	public ResponseEntity<BlogPostDetailsResponse> getPostsByUserId(@PathVariable long userId) {
		User u = s.getbyId(userId);
		logger.info("getting the post");
		return new ResponseEntity<BlogPostDetailsResponse>(BlogPostDetailsResponse.convertBlogPostDetailsResponse(u),
				HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> getById(@PathVariable long id) {
		User u = s.getbyId(id);
		logger.info("getting the user details");
		return new ResponseEntity<UserResponse>(UserResponse.convertUserResponse(u), HttpStatus.OK);
	}

	@GetMapping
	public ResponseEntity<List<UserResponse>> getAll() {
		logger.info("getting all the user details");
		return new ResponseEntity<List<UserResponse>>(s.getAll(), HttpStatus.OK);
	}


	@PutMapping
	public ResponseEntity<UserResponse> updateUser( @RequestBody UserDTO userDTO) throws JsonProcessingException {
		User user = s.updateUser(userDTO);
		return new ResponseEntity<UserResponse>(UserResponse.convertUserResponse(user),HttpStatus.OK);
	}


	@DeleteMapping("/{id}")
	public  ResponseEntity<UserResponse> deleteUser(  @PathVariable  long id) throws JsonProcessingException{
		logger.info("deleting in progress...");
		return new ResponseEntity<UserResponse>(s.deleteUser(id),HttpStatus.OK);

    }



}

