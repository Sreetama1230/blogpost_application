package com.example.demo.dto;



public class CommentDTO {
	private Long id;
	private String content;
	public CommentDTO(Long id, String content) {
		super();
		this.id = id;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}



	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public CommentDTO() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CommentDTO(String content) {
		super();
		this.content = content;
	}


	
	
	
}
