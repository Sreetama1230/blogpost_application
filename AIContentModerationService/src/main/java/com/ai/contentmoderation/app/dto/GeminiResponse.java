package com.ai.contentmoderation.app.dto;

import java.util.List;

public class GeminiResponse {

	private List<Candidate> candidates;

	public List<Candidate> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<Candidate> candidates) {
		this.candidates = candidates;
	}

	public GeminiResponse(List<Candidate> candidates) {
		super();
		this.candidates = candidates;
	}
	
	
}
