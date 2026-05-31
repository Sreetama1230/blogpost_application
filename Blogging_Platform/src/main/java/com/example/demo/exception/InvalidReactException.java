package com.example.demo.exception;

public class InvalidReactException extends RuntimeException{
	
	public InvalidReactException(){
		super();
	}

	public InvalidReactException(String str){
		super(str);
	}
}
