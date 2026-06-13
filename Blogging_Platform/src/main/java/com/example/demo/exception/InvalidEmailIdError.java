package com.example.demo.exception;

public class InvalidEmailIdError extends RuntimeException{
	
	public InvalidEmailIdError(){
		super();
	}
	
	public InvalidEmailIdError(String message){
		super(message);
	}

}
