package com.example.demo.response;

import java.time.LocalDateTime;

import com.example.demo.model.Comment;




public class CommentResponse {
	private String content;

	private Long id;
	private LocalDateTime createAt;


	private Long likeCount;

	private Long loveCount;

	private Long funnyCount;

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

	

	public Long getLikeCount() {
		return likeCount;
	}


	public void setLikeCount(Long likeCount) {
		this.likeCount = likeCount;
	}


	public Long getLoveCount() {
		return loveCount;
	}


	public void setLoveCount(Long loveCount) {
		this.loveCount = loveCount;
	}


	public Long getFunnyCount() {
		return funnyCount;
	}


	public void setFunnyCount(Long funnyCount) {
		this.funnyCount = funnyCount;
	}

	

	public CommentResponse(String content, LocalDateTime createAt, Long likeCount, Long loveCount, Long funnyCount) {
		super();
		this.content = content;
		this.createAt = createAt;
		this.likeCount = likeCount;
		this.loveCount = loveCount;
		this.funnyCount = funnyCount;
	}


	public CommentResponse(String content, Long id, LocalDateTime createAt, Long likeCount, Long loveCount,
			Long funnyCount) {
		super();
		this.content = content;
		this.id = id;
		this.createAt = createAt;
		this.likeCount = likeCount;
		this.loveCount = loveCount;
		this.funnyCount = funnyCount;
	}


	public static CommentResponse convertCommentResponse(Comment c) {
		return new CommentResponse(c.getContent(), c.getId(),c.getCreateAt(),c.getLikeCount(),c.getLoveCount(),c.getFunnyCount());
	}
}
