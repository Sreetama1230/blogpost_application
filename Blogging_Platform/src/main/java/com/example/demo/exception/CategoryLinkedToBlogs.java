package com.example.demo.exception;

public class CategoryLinkedToBlogs  extends RuntimeException{ 
	
	public CategoryLinkedToBlogs() {

	}

	public CategoryLinkedToBlogs( String msg) {
		 super(msg);
	}

}
