package com.ai.contentmoderation.app.dto;

public class Candidate {

	private Content content;

	public Candidate(Content content) {
		super();
		this.content = content;
	}

	public Candidate() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}
	
	
}
