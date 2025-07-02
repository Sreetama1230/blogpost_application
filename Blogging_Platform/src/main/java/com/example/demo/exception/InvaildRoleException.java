package com.example.demo.exception;

public class InvaildRoleException extends RuntimeException {
	public InvaildRoleException(String msg) {

		super(msg);
	}

	public InvaildRoleException() {

		super();
	}

}