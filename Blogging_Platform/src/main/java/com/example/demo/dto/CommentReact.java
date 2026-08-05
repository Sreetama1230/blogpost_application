package com.example.demo.dto;

import com.example.demo.enums.Reaction;

public class CommentReact {

	private long commentId;
	private Reaction reaction;
	private Long syncToken;
	
	public long getId() {
		return commentId;
	}
	public void setId(long commentId) {
		this.commentId = commentId;
	}
	public Reaction getReaction() {
		return reaction;
	}
	public void setReaction(Reaction reaction) {
		this.reaction = reaction;
	}
	
	public long getCommentId() {
		return commentId;
	}
	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}
	
	public Long getSyncToken() {
		return syncToken;
	}
	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}
	public CommentReact() {
		super();
	}
	
	public CommentReact(long commentId, Reaction reaction) {
		super();
		this.commentId = commentId;
		this.reaction = reaction;
	}
	public CommentReact(long commentId, Reaction reaction, Long syncToken) {
		super();
		this.commentId = commentId;
		this.reaction = reaction;
		this.syncToken = syncToken;
	}
	
	
}
