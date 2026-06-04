package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.example.demo.config.SecurityUtils;
import com.example.demo.constants.AppConstants;
import com.example.demo.constants.Reaction;

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
	private KafkaTemplate<String,String> kafkaTemplate;
	Logger logger = LoggerFactory.getLogger(Comment.class);



	@Transactional
	public BlogPostResponse createOrUpdateComment( CommentDTO c, long blogPostId) {
		long userId = SecurityUtils.getCurrentUserId();
		
		if(!blogPostDao.findById(blogPostId).isPresent()) {
			throw new ResourceNotFoundException("no blog post found with this id");
		}
		BlogPost dbBlogPost = blogPostDao.findById(blogPostId).get();
		User authDbUser = userdao.findById(userId).get();
		logger.info("added new comment : {}", c.getMessage());
		List<Comment> exitsingComments = dbBlogPost.getComments();
		List<Comment> listComments = new ArrayList<>();
		if (exitsingComments != null) {
			listComments.addAll(exitsingComments);
		}

		Comment newComment = new Comment( c.getMessage(), authDbUser, dbBlogPost, LocalDateTime.now(),0L,0L,0L, new TreeSet<>());

		Comment existingComment;
		// an existing comment can be changed only by that particular user

		if (  c.getCommentId() != null && c.getCommentId()>0) {

			if(commentDao.findById(c.getCommentId()).isEmpty()){
				throw new ResourceNotFoundException("No comment is present with the provided id...");
			}else{
				User commentAuthor = commentDao.findById(c.getCommentId()).get().getUser();
				if (canUpdateOrDelete(authDbUser,commentAuthor) ) {
					// update

					existingComment = commentDao.findById(c.getCommentId()).get();


					if(!existingComment.getContent().equals(c.getMessage())){
						existingComment.setContent(c.getMessage()+"(edited)");

					}

					commentDao.save(existingComment);

					try {
						SendResult<String, String> res = kafkaTemplate.
								send(AppConstants.ADMINTOOL_TOPIC_NAME,
								"Updated Comment " + String.valueOf(existingComment.getId())).get();
					
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					throw new DoNotHavePermissionError("You can not make changes!");
				}
			}



		}else{
			Comment savedComment = commentDao.save(newComment);
			
			try {
				SendResult<String, String> res = kafkaTemplate.
						send(AppConstants.ADMINTOOL_TOPIC_NAME,
						"Created Comment " + String.valueOf(savedComment.getId())).get();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			listComments.add(newComment);
		}

		dbBlogPost.setComments(listComments);
		BlogPost newBlogPostWithComment = blogPostDao.save(dbBlogPost);
		authDbUser.setComments(listComments);
		userdao.save(authDbUser);

		return BlogPostResponse.convertBlogPostRespons(newBlogPostWithComment);
	}

	@Transactional
	public CommentResponse deleteComment( long id, long blogpostid) {

			long userId = SecurityUtils.getCurrentUserId();

			if ( commentDao.findCommentByIdAndBlogPostId(id, blogpostid).isPresent()) {
				Comment c = commentDao.findCommentByIdAndBlogPostId(id, blogpostid).get();
				User authDbUser = userdao.findById(userId).get();
				User commentAuthor = userdao.findById(c.getUser().getId()).get();
				// authenticated user vs comment's author
				if (canUpdateOrDelete(authDbUser,commentAuthor)) {
					CommentResponse cr = CommentResponse.convertCommentResponse(c);
					BlogPost blogPost = blogPostDao.findById(blogpostid).get();
					blogPost.getComments().remove(c);
					commentAuthor.getComments().remove(c);
					commentDao.deleteById(c.getId());
					try {
						SendResult<String, String> res = kafkaTemplate.
								send(AppConstants.ADMINTOOL_TOPIC_NAME,
										"Deleted Comment " + String.valueOf(c.getId())).get();
					
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					return cr;
				} else {
					throw new DoNotHavePermissionError("You are not allowed to change other's comment!");
				}

			} else {
				throw new ResourceNotFoundException("Resource Not Present...");
			}

	}
	
	
	public CommentResponse getById(long id) {
		if(commentDao.findById(id).isPresent()) {
			return  CommentResponse.convertCommentResponse(commentDao.findById(id).get());
		}else {
			throw new ResourceNotFoundException("Resource is not present!");
		}
	}

	@Transactional
	public CommentResponse reactComment(CommentReact commentReact) {
		long id = SecurityUtils.getCurrentUserId();
		User dbUser = userdao.findById(id).get();
		
		if(commentDao.findById(commentReact.getId()).isPresent()) {
			
			Comment dbComment = commentDao.findById(commentReact.getId()).get();
	
			if(dbComment.getReactedUsers().contains(dbUser)) {
				
				throw new InvalidReactException("you have already reacted!");
				
			}
			switch(commentReact.getReaction()) {
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
			try {
				SendResult<String, String> res = kafkaTemplate.
						send(AppConstants.ADMINTOOL_TOPIC_NAME,
						"Reacted Comment " + String.valueOf(commentReact.getId())).get();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			return  CommentResponse.convertCommentResponse(dbComment);
		}else {
			throw new ResourceNotFoundException("Resource is not present!");
		}
	}
	
//	@Transactional
//	public CommentResponse removeReactedComment(long commentId) {
//		long id = SecurityUtils.getCurrentUserId();
//		User dbUser = userdao.findById(id).get();
//		
//		
//		if(commentDao.findById(commentId).isPresent()) {
//			
//			Comment dbComment = commentDao.findById(commentId).get();
//	
//
//			if(dbComment.getReactedUsers().contains(dbUser)) {
//				
//				
//				dbComment.getReactedUsers().remove(dbUser);
//				
//				
//				switch(commentId.getReaction()) {
//				case FUNNY:
//					dbComment.setFunnyCount(dbComment.getFunnyCount() - 1);
//					break;
//				case LIKE:
//					dbComment.setLikeCount(dbComment.getLikeCount() - 1);
//					break;
//				case LOVE:
//					dbComment.setLoveCount(dbComment.getLoveCount() - 1);
//					break;
//				default:
//					throw new InvalidReactException("Invalid request : please provide a valid react");
//				
//				}
//				
//				}else {
//					throw new InvalidReactException("You have not reacted this comment!");
//				}
//			
//			commentDao.save(dbComment);
//			return  CommentResponse.convertCommentResponse(dbComment);
//		}else {
//			throw new ResourceNotFoundException("Resource is not present!");
//		}
//	}
	
	
	public static boolean canUpdateOrDelete(User authDbUser, User commentAuthor){
		if(SecurityUtils.isUser(authDbUser)){
			return authDbUser.getId().equals(commentAuthor.getId());
		}
		//any comment can be edited by admin
		// one admin's comment can not be edited by another admin
		if (SecurityUtils.isAdmin(authDbUser)) {
			// own                                                   // editor or user
			return authDbUser.getId().equals(commentAuthor.getId())
					|| SecurityUtils.isEditor(commentAuthor) || SecurityUtils.isUser(commentAuthor);
		}

		// Editors can only update their own comments
		if (SecurityUtils.isEditor(authDbUser)) {
			return authDbUser.getId().equals(commentAuthor.getId());
		}

		return false;
	}
}
