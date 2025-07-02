package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.error.ErrorDetails;

@ControllerAdvice
public class CategoryLinkedToBlogsHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorDetails> handleCategoryLinkedToBlogs(CategoryLinkedToBlogs categoryLinkedToBlogs) {
		ErrorDetails details = new ErrorDetails(categoryLinkedToBlogs.getMessage(), HttpStatus.BAD_REQUEST.value());
		return new ResponseEntity<ErrorDetails>(details, HttpStatus.BAD_REQUEST);
	}
}
