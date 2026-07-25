package com.example.demo.dto;


import java.util.List;

public class ModerationRequest {

	private String title;
	private String content;
	private List<String> categories;
	
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

	
	
	public ModerationRequest(String title, String content, List<String> categories) {
		super();
		this.title = title;
		this.content = content;
		this.categories = categories;
	}
	public List<String> getCategories() {
		return categories;
	}
	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
	public ModerationRequest() {
		super();
		// TODO Auto-generated constructor stub
	}
	
}
