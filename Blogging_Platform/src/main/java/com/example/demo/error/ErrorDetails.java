package com.example.demo.error;

public class ErrorDetails {
	private String msg;
	private int status;

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
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
		this.msg = msg;
		this.status = status;
	}

}
