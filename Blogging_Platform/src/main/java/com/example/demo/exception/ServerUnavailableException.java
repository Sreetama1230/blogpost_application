package com.example.demo.exception;

public class ServerUnavailableException extends RuntimeException {
	
	public ServerUnavailableException(String msg){
		super(msg);
	}

}
