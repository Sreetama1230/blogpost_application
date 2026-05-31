package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.error.ErrorDetails;

@ControllerAdvice
public class CustomExceptionHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleUnexpectedCustomException(UnexpectedCustomException e) {
		ErrorDetails ed = new ErrorDetails(e.getMessage(), HttpStatus.CONFLICT.value());
		return new ResponseEntity<ErrorDetails>(ed, HttpStatus.CONFLICT);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleInvalidReactException(InvalidReactException e) {
		ErrorDetails ed = new ErrorDetails(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return new ResponseEntity<ErrorDetails>(ed, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handlerCategoryException(CategoryException ce) {
		ErrorDetails ed = new ErrorDetails(ce.getMessage(), HttpStatus.BAD_REQUEST.value());

		return new ResponseEntity<>(ed, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleCategoryLinkedToBlogs(CategoryLinkedToBlogs categoryLinkedToBlogs) {
		ErrorDetails details = new ErrorDetails(categoryLinkedToBlogs.getMessage(), HttpStatus.BAD_REQUEST.value());
		return new ResponseEntity<ErrorDetails>(details, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleDoNotHavePermissionError(DoNotHavePermissionError e) {
		return new ResponseEntity<ErrorDetails>(
				new ErrorDetails("You don't have permission!", HttpStatus.FORBIDDEN.value()), HttpStatus.FORBIDDEN);

	}
	
	@ExceptionHandler
	public ResponseEntity<ErrorDetails>  handleInvaildRoleException(InvaildRoleException exception){
		return new ResponseEntity<ErrorDetails>(new ErrorDetails("Invalid Role", HttpStatus.BAD_REQUEST.value()),HttpStatus.BAD_REQUEST);

	}
	
	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException e) {
		ErrorDetails ed = new ErrorDetails(e.getMessage(), HttpStatus.NOT_FOUND.value());
		return new ResponseEntity<>(ed, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleInvalidEmailIdError(InvalidEmailIdError e) {
		ErrorDetails ed = new ErrorDetails(e.getMessage(), HttpStatus.BAD_REQUEST.value());
		return new ResponseEntity<>(ed, HttpStatus.BAD_REQUEST);
	}
}
