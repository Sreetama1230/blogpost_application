package com.example.demo.dto;

import com.example.demo.constants.Reaction;

public class CommentReact {

	private long commentId;
	private Reaction reaction;

	
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
	public CommentReact() {
		super();
	}
	public CommentReact(long commentId, Reaction reaction) {
		super();
		this.commentId = commentId;
		this.reaction = reaction;
	}
	
	
}
