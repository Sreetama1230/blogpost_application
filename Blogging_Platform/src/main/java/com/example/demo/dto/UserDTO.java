package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDTO {

	private Long id;
	@NotBlank(message = "Username is required and must be unique")
	private String username;
    @NotBlank(message = "Password is required")
	private String password;
    @Email(message = "Invalid email format")
	private String email;
	private String bio;
	@NotBlank(message = "Please provide a role")
	private Set<String> roles = new HashSet<>();


	public UserDTO(@NotBlank(message = "Username is required") String username,
			@NotBlank(message = "Password is required") String password,
			@Email(message = "Invalid email format") String email, String bio) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
		this.bio = bio;
	}

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
}
