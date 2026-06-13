package com.example.demo.error;

public class ErrorDetails {
	private String message;
	private int status;

	public String getMsg() {
		return message;
	}

	public void setMsg(String msg) {
		this.message = msg;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public ErrorDetails() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ErrorDetails(String msg, int status) {
		super();
		this.message = msg;
		this.status = status;
	}

}
