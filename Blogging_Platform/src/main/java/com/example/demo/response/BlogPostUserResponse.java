package com.example.demo.response;

import com.example.demo.model.User;

public class BlogPostUserResponse {

	private String username;
	private Long id;

	private String email;


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BlogPostUserResponse(String username, String email, Long id) {
		this.username = username;
		this.email = email;
		this.id = id;
	}

	public BlogPostUserResponse() {
		super();

	}


	public BlogPostUserResponse(String username, String email) {
		super();
		this.username = username;
		this.email = email;
	}
	
	public static BlogPostUserResponse convertBlogPostUserResponse(User u) {
		return new BlogPostUserResponse(u.getUsername(), u.getEmail(),u.getId());
	}
}
