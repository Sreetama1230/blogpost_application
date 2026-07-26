package com.example.demo.exception;

public class HarmfulContentException  extends RuntimeException{

	HarmfulContentException(){
		
	}
	public HarmfulContentException(String msg){
		super(msg);
	}
}
