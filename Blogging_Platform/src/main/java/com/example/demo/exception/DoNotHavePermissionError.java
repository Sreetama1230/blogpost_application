package com.example.demo.exception;

public class DoNotHavePermissionError extends RuntimeException {
	public DoNotHavePermissionError(String str) {

		super(str);
	}

	public DoNotHavePermissionError() {
		
	}
}
