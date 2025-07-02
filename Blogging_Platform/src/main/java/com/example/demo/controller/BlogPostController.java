package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BlogPostDTO;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.UserService;

@RestController
@RequestMapping("/blogs")
public class BlogPostController {

	@Autowired
	private BlogPostService service;
	
	  Logger logger
				= LoggerFactory.getLogger(BlogPostController.class);


	@PostMapping
	public ResponseEntity<BlogPostResponse> createBlogPost( @RequestBody BlogPostDTO bp) {
  	return new ResponseEntity<>(service.createOrUpdateBlogPost(bp),HttpStatus.CREATED);
	}
	
	@GetMapping("title/{title}/user/{userId}")
	public  ResponseEntity<List<BlogPostResponse>> getBlogsByTitleAndUserId(  @PathVariable  String title, @PathVariable Long userId){
		return new ResponseEntity<>(service.getBlogsByTitleAndUserId(title, userId),HttpStatus.OK);
	}
	
	@PutMapping
	public ResponseEntity<BlogPostResponse> updateBlogPost(@RequestBody BlogPostDTO bp) {
			logger.info("Updating blog post...");
		return new ResponseEntity<BlogPostResponse>(service.createOrUpdateBlogPost( bp),HttpStatus.CREATED);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<BlogPostResponse> deleteBlogPost(@PathVariable long id){
		logger.info("Deleting blog post, id: {}",id);
		return new ResponseEntity<BlogPostResponse>(service.deleteBlogPost(id),HttpStatus.OK);
	}
	
	
}




