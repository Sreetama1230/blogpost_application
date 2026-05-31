package com.example.demo.exception;

public class InvalidEmailIdError extends RuntimeException{
	
	InvalidEmailIdError(){
		super();
	}
	
	InvalidEmailIdError(String message){
		super(message);
	}

}
