package com.example.demo.response;

import java.time.LocalDateTime;

import com.example.demo.model.Comment;




public class CommentResponse {
	private String content;

	private Long id;
	private LocalDateTime createAt;


	public String getContent() {
		return content;
	}


	public void setContent(String content) {
		this.content = content;
	}




	public LocalDateTime getCreateAt() {
		return createAt;
	}


	public void setCreateAt(LocalDateTime createAt) {
		this.createAt = createAt;
	}



	public CommentResponse(String content, LocalDateTime createAt) {
		super();
		this.content = content;
		this.createAt = createAt;
	}


	public CommentResponse() {
		super();

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CommentResponse(LocalDateTime createAt, String content, Long id) {
		this.createAt = createAt;
		this.content = content;
		this.id = id;
	}

	public static CommentResponse convertCommentResponse(Comment c) {
		return new CommentResponse(c.getCreateAt(),c.getContent(), c.getId());
	}
}
