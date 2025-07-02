package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CommentDTO;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;
import com.example.demo.service.CommentService;

@RestController
@RequestMapping("/comments")
public class CommentController {

	@Autowired
	private CommentService commentService;

	Logger logger = LoggerFactory.getLogger(CommentController.class);

	@PostMapping
	public ResponseEntity<BlogPostResponse> addComments(@RequestBody CommentDTO c,
			@RequestParam long blogPostId) {

		return new ResponseEntity<BlogPostResponse>(commentService.createOrUpdateComment( c, blogPostId),
				HttpStatus.CREATED);
	}

	@PutMapping
	public ResponseEntity<BlogPostResponse> updateComments(@RequestBody CommentDTO c, @RequestParam long blogPostId) {
		logger.info("edited comment {}", c.getContent());
		return new ResponseEntity<BlogPostResponse>(commentService.createOrUpdateComment(c, blogPostId), HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CommentResponse> getById(  @PathVariable long id){
		return new ResponseEntity<CommentResponse>(commentService.getById(id),HttpStatus.OK);
	}
	
	
	
	@DeleteMapping("/{cId}/blogposts/{bpId}")
	public ResponseEntity<CommentResponse> deleteComment(  @PathVariable long cId, @PathVariable long bpId) {
		logger.info("deleted comment {id}"+cId);
		return new ResponseEntity<CommentResponse>(commentService.deleteComment( cId, bpId), HttpStatus.OK);
	}
}
