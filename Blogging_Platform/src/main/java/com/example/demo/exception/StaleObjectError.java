package com.example.demo.exception;

public class StaleObjectError extends RuntimeException{

	public StaleObjectError(String msg) {
		super(msg);
	}
}
