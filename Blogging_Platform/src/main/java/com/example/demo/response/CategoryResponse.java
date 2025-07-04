package com.example.demo.response;

import com.example.demo.model.Category;

public class CategoryResponse {


	private Long id;
	private String name;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public CategoryResponse() {
		super();

	}
	public CategoryResponse(Long id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	public static CategoryResponse  convertCategoryResponse(Category c) {
		return new CategoryResponse (c.getId(),c.getName());
	}
}
