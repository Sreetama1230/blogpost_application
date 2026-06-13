package com.example.demo.dto;

import com.example.demo.model.Category;

public class CategoryDTO {

	private String name;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public CategoryDTO( String name) {
		super();

		this.name = name;
	}
	public CategoryDTO() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public static CategoryDTO convertToCategoryDTO(Category category) {
		CategoryDTO dto = 	new CategoryDTO();
		dto.setName(category.getName());
		return dto ;
	}
	
}
