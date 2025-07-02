package com.example.demo.exception;

public class CategoryException extends RuntimeException{

   public CategoryException(String str){
        super(str);
    }

    public CategoryException(){
        super();
    }
}
