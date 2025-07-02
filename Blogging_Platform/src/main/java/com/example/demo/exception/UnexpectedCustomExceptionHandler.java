package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.error.ErrorDetails;

@ControllerAdvice
public class UnexpectedCustomExceptionHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleUnexpectedCustomException(UnexpectedCustomException e){
		ErrorDetails ed = new ErrorDetails(e.getMessage(), HttpStatus.CONFLICT.value());
		return new ResponseEntity<ErrorDetails>(ed,HttpStatus.CONFLICT);
	}
}
