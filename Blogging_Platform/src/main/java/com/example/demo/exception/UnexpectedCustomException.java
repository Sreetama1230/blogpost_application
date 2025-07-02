package com.example.demo.exception;

public class UnexpectedCustomException  extends RuntimeException{

	public UnexpectedCustomException () {
		
	}
	public UnexpectedCustomException (String s) {
		super(s);
	}
}
