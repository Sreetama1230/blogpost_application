package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;


public class BlogPostDTO {
 
	private long id;
	private String title;

	private String content;

	private Set<CategoryDTO> categories = new HashSet<>();

	private Long syncToken;
	
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public BlogPostDTO() {
		super();
		// TODO Auto-generated constructor stub
	}


	public BlogPostDTO(long id, String title, String content, Set<CategoryDTO> categories) {
		super();
		this.id = id;
		this.title = title;
		this.content = content;
		this.categories = categories;

	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Set<CategoryDTO> getCategories() {
		return categories;
	}

	public void setCategories(Set<CategoryDTO> categories) {
		this.categories = categories;
	}

	public Long getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}

	public BlogPostDTO(String title, String content, Set<CategoryDTO> categories, Long syncToken) {
		super();
		this.title = title;
		this.content = content;
		this.categories = categories;
		this.syncToken = syncToken;
	}




}
