package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.error.ErrorDetails;

@ControllerAdvice
public class DoNotHavePermissionErrorHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleDoNotHavePermissionError(DoNotHavePermissionError e) {
		return new ResponseEntity<ErrorDetails>
		(new ErrorDetails("You don't have permission!", HttpStatus.FORBIDDEN.value()), HttpStatus.FORBIDDEN);

	}
}
