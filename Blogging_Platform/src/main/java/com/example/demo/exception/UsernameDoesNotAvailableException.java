package com.example.demo.exception;

public class UsernameDoesNotAvailableException extends RuntimeException{

	public UsernameDoesNotAvailableException(String msg) {
		super(msg);
	}
}
