package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.error.ErrorDetails;

@ControllerAdvice
public class ResourceNotFoundExceptionHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException e) {
		ErrorDetails ed = new ErrorDetails(e.getMessage(), HttpStatus.NOT_FOUND.value());
		return new ResponseEntity<>(ed, HttpStatus.NOT_FOUND);
	}
}
