package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDTO {

	private Long id;
	private String username;
	private String password;
	private String email;
	private String bio;
	private Set<String> roles = new HashSet<>();

	public UserDTO(String username, String password, String email, String bio) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
		this.bio = bio;
	}

	private Long syncToken;

	public String getBio() {
		return bio;
	}

	public void setBio(String dio) {
		this.bio = dio;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}

	public UserDTO(String username, String password, String email) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
	}

	public UserDTO() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserDTO(String username, String password, String email, String bio, Set<String> roles, Long syncToken) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
		this.bio = bio;
		this.roles = roles;
		this.syncToken = syncToken;
	}
	
	
}
