package com.example.demo.dto;



public class CommentDTO {
	private Long commentId;
	private String message;
	private Long syncToken;
	
	public Long getCommentId() {
		return commentId;
	}
	public void setCommentId(Long commentId) {
		this.commentId = commentId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	
	public Long getSyncToken() {
		return syncToken;
	}
	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}
	public CommentDTO(Long commentId, String message) {
		super();
		this.commentId = commentId;
		this.message = message;
	}
	public CommentDTO() {
		super();
		// TODO Auto-generated constructor stub
	}
	public CommentDTO(Long commentId, String message, Long syncToken) {
		super();
		this.commentId = commentId;
		this.message = message;
		this.syncToken = syncToken;
	}
	
	
}
