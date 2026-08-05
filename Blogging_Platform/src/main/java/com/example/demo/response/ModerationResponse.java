package com.example.demo.response;

public class ModerationResponse {

	private String response;
	private boolean isApproved;
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}
	public boolean isApproved() {
		return isApproved;
	}
	public void setApproved(boolean isApproved) {
		this.isApproved = isApproved;
	}
	public ModerationResponse() {
		super();
		// TODO Auto-generated constructor stub
	}
	public ModerationResponse(String response, boolean isApproved) {
		super();
		this.response = response;
		this.isApproved = isApproved;
	}
	
	
	
}
