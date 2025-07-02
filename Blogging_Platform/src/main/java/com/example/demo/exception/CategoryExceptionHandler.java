package com.example.demo.exception;

import com.example.demo.error.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CategoryExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorDetails> handlerCategoryException(CategoryException ce){
        ErrorDetails ed =   new ErrorDetails(ce.getMessage(), HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(ed,HttpStatus.BAD_REQUEST);
    }
}
