package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import com.example.demo.config.SecurityUtils;
import com.example.demo.constants.AppConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.CommentDTO;
import com.example.demo.dto.CommentReact;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvalidReactException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;
import com.example.demo.model.*;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;

import jakarta.transaction.Transactional;

@Service
public class CommentService {

	@Autowired
	private CommentDao commentDao;

	@Autowired
	private BlogPostDao blogPostDao;

	@Autowired
	private UserDao userdao;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	Logger logger = LoggerFactory.getLogger(Comment.class);

	@Transactional
	public BlogPostResponse createOrUpdateComment(CommentDTO c, long blogPostId) {
		
		logger.info("request started processing for " + c.getMessage());
		
		long userId = SecurityUtils.getCurrentUserId();

		if (!blogPostDao.findById(blogPostId).isPresent()) {
			throw new ResourceNotFoundException("no blog post found with this id");
		}
		
		BlogPost dbBlogPost = blogPostDao.findById(blogPostId).get();
		
		User authDbUser = userdao.findById(userId).get();
		
		logger.info("adding new comment : {}", c.getMessage());
		
		List<Comment> exitsingComments = dbBlogPost.getComments();
		
		Comment newComment = new Comment(c.getMessage(), authDbUser, dbBlogPost, LocalDateTime.now(), 0L, 0L, 0L,
				new TreeSet<>());

		// an existing comment can be changed only by the writer of that comment

		if (c.getCommentId() != null && c.getCommentId() > 0) {

			Comment existingComment;
			if (commentDao.findById(c.getCommentId()).isEmpty()) {
				throw new ResourceNotFoundException("No comment is present with the provided id...");
			} else {

				User commentAuthor = commentDao.findById(c.getCommentId()).get().getUser();
				if (canUpdateOrDelete(authDbUser, commentAuthor)) {
					// update

					existingComment = commentDao.findById(c.getCommentId()).get();

					if (!existingComment.getContent().equals(c.getMessage())) { // if the message did not change
						existingComment.setContent(c.getMessage() + "(edited)");

						commentDao.save(existingComment);

						CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
								"Updated Comment " + String.valueOf(existingComment.getId()));
						
						future.whenComplete((result , exception) ->{
							if(exception != null) {
								throw new UnexpectedCustomException("Error occured while publishing the event");
							}else {
								logger.info("Successfully published the event...");
							}
						});

					}

				} else {
					throw new DoNotHavePermissionError("You can not make changes on this comment!");
				}

			}

		} else {
			Comment savedComment = commentDao.save(newComment);

//			try {
//				SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
//						"Created Comment " + String.valueOf(savedComment.getId())).get();
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
			CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
					"Created Comment " + String.valueOf(savedComment.getId()));
			
			future.whenComplete((result , exception) ->{
				if(exception != null) {
					throw new UnexpectedCustomException("Error occured while publishing the event");
				}else {
					logger.info("Successfully published the event...");
				}
			});


			exitsingComments.add(newComment);
		}

		BlogPost newBlogPostWithComment = blogPostDao.save(dbBlogPost);
		authDbUser.getComments().add(newComment);
		userdao.save(authDbUser);
		logger.info("returning response... " + newBlogPostWithComment.getContent());
		return BlogPostResponse.convertBlogPostRespons(newBlogPostWithComment);
	}

	@Transactional
	public CommentResponse deleteComment(long id, long blogpostid) {

		long userId = SecurityUtils.getCurrentUserId();

		if (commentDao.findCommentByIdAndBlogPostId(id, blogpostid).isPresent()) {
			Comment c = commentDao.findCommentByIdAndBlogPostId(id, blogpostid).get();
			User authDbUser = userdao.findById(userId).get();
			User commentAuthor = userdao.findById(c.getUser().getId()).get();
			// authenticated user vs comment's author
			if (canUpdateOrDelete(authDbUser, commentAuthor)) {
				CommentResponse cr = CommentResponse.convertCommentResponse(c);
				BlogPost blogPost = blogPostDao.findById(blogpostid).get();
				blogPost.getComments().remove(c);
				commentAuthor.getComments().remove(c);
				commentDao.deleteById(c.getId());
				
//				try {
//					SendResult<String, String> res = kafkaTemplate
//							.send(AppConstants.ADMINTOOL_TOPIC_NAME, "Deleted Comment " + String.valueOf(c.getId()))
//							.get();
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				
				
				CompletableFuture<SendResult<String, String>> future =  kafkaTemplate
						.send(AppConstants.ADMINTOOL_TOPIC_NAME, "Deleted Comment " + String.valueOf(c.getId()));
				
				future.whenComplete((result , exception) ->{
					if(exception != null) {
						throw new UnexpectedCustomException("Error occured while publishing the event");
					}else {
						logger.info("Successfully published the event...");
					}
				});

				return cr;
			} else {
				throw new DoNotHavePermissionError("You are not allowed to change other's comment!");
			}

		} else {
			throw new ResourceNotFoundException("Resource is not present...");
		}

	}

	public CommentResponse getById(long id) {
		if (commentDao.findById(id).isPresent()) {
			return CommentResponse.convertCommentResponse(commentDao.findById(id).get());
		} else {
			throw new ResourceNotFoundException("Resource is not present!");
		}
	}

	@Transactional
	public CommentResponse reactComment(CommentReact commentReact) {
		long id = SecurityUtils.getCurrentUserId();
		User dbUser = userdao.findById(id).get();

		if (commentDao.findById(commentReact.getId()).isPresent()) {

			Comment dbComment = commentDao.findById(commentReact.getId()).get();

			if (dbComment.getReactedUsers().contains(dbUser)) {

				throw new InvalidReactException("You have already reacted!");

			}
			switch (commentReact.getReaction()) {
			case FUNNY:
				dbComment.setFunnyCount(dbComment.getFunnyCount() + 1);
				break;
			case LIKE:
				dbComment.setLikeCount(dbComment.getLikeCount() + 1);
				break;
			case LOVE:
				dbComment.setLoveCount(dbComment.getLoveCount() + 1);
				break;
			default:
				throw new InvalidReactException("Invalid request : please provide a valid react");
			}

			dbComment.getReactedUsers().add(dbUser);
			commentDao.save(dbComment);
			
			
//			try {
//				SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
//						"Reacted Comment " + String.valueOf(commentReact.getId())).get();
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			
			CompletableFuture<SendResult<String, String>> future =  kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
					"Reacted Comment " + String.valueOf(commentReact.getId()));
			
			future.whenComplete((result , exception) ->{
				if(exception != null) {
					throw new UnexpectedCustomException("Error occured while publishing the event");
				}else {
					logger.info("Successfully published the event...");
				}
			});			
			
			
			return CommentResponse.convertCommentResponse(dbComment);
		} else {
			throw new ResourceNotFoundException("Resource is not present!");
		}
	}

	public static boolean canUpdateOrDelete(User authDbUser, User commentAuthor) {
		if (SecurityUtils.isUser(authDbUser)) {
			return authDbUser.getId().equals(commentAuthor.getId());
		}
		// any comment can be edited by admin
		// one admin's comment can not be edited by another admin
		if (SecurityUtils.isAdmin(authDbUser)) {
			// own // editor or user
			return authDbUser.getId().equals(commentAuthor.getId()) || SecurityUtils.isEditor(commentAuthor)
					|| SecurityUtils.isUser(commentAuthor);
		}

		// Editors can only update their own comments
		if (SecurityUtils.isEditor(authDbUser)) {
			return authDbUser.getId().equals(commentAuthor.getId());
		}

		return false;
	}
}
