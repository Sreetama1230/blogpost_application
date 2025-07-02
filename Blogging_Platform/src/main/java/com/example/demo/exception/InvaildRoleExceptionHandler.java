package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.error.ErrorDetails;

@ControllerAdvice
public class InvaildRoleExceptionHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorDetails>  handleInvaildRoleException(InvaildRoleException exception){
		return new ResponseEntity<ErrorDetails>(new ErrorDetails("Invalid Role", HttpStatus.BAD_REQUEST.value()),HttpStatus.BAD_REQUEST);

	}
}
