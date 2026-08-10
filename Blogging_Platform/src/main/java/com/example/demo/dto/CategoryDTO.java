package com.example.demo.dto;

import com.example.demo.model.Category;

public class CategoryDTO {

	private String name;
	private Long syncToken;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CategoryDTO(String name) {
		super();

		this.name = name;
	}

	public Long getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}

	public CategoryDTO() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CategoryDTO(String name, Long syncToken) {
		super();
		this.name = name;
		this.syncToken = syncToken;
	}

	public static CategoryDTO convertToCategoryDTO(Category category) {
		CategoryDTO dto = new CategoryDTO();
		dto.setName(category.getName());
		dto.setSyncToken(category.getSyncToken());
		return dto;
	}

}
