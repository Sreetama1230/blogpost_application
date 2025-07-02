package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.config.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.CommentDTO;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;

@Service
public class CommentService {

	@Autowired
	private CommentDao commentDao;

	@Autowired
	private BlogPostDao blogPostDao;

	@Autowired
	private UserDao userdao;

	Logger logger = LoggerFactory.getLogger(Comment.class);

	public BlogPostResponse createOrUpdateComment( CommentDTO c, long blogPostId) {
		long userId = SecurityUtils.getCurrentUserId();
		BlogPost dbBlogPost = blogPostDao.findById(blogPostId).get();
		User authDbUser = userdao.findById(userId).get();
		logger.info("added new comment : {}", c.getContent());
		List<Comment> exitsingComments = dbBlogPost.getComments();
		List<Comment> listComments = new ArrayList<>();
		if (exitsingComments != null) {
			listComments.addAll(exitsingComments);
		}

		Comment newComment = new Comment(c.getContent(), authDbUser, dbBlogPost, LocalDateTime.now());

		Comment existingComment;
		// an existing comment can be changed only by that particular user

		if (  c.getId() != null && c.getId()>0) {

			if(commentDao.findById(c.getId()).isEmpty()){
				throw new ResourceNotFoundException("No comment is present with the provided id...");
			}else{
				User commentAuthor = commentDao.findById(c.getId()).get().getUser();
				if (canUpdateOrDelete(authDbUser,commentAuthor) ) {
					// update

					existingComment = commentDao.findById(c.getId()).get();


					if(!existingComment.getContent().equals(c.getContent())){
						existingComment.setContent(c.getContent()+"(edited)");

					}

					commentDao.save(existingComment);


				} else {
					throw new DoNotHavePermissionError("You can not make changes!");
				}
			}



		}else{
			commentDao.save(newComment);
			listComments.add(newComment);
		}

		dbBlogPost.setComments(listComments);
		BlogPost newBlogPostWithComment = blogPostDao.save(dbBlogPost);
		authDbUser.setComments(listComments);
		userdao.save(authDbUser);

		return BlogPostResponse.convertBlogPostRespons(newBlogPostWithComment);
	}

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

	public static boolean canUpdateOrDelete(User authDbUser, User commentAuthor){
		if(SecurityUtils.isUser(authDbUser)){
			return authDbUser.getId().equals(commentAuthor.getId());
		}
		//any comment can be edited by admin
		if (SecurityUtils.isAdmin(authDbUser)) {
			// own                                                   // editor or user
			return authDbUser.getId().equals(commentAuthor.getId())
					|| SecurityUtils.isEditor(commentAuthor) || SecurityUtils.isUser(commentAuthor);
		}

		// Editors can only update themselves
		if (SecurityUtils.isEditor(authDbUser)) {
			return authDbUser.getId().equals(commentAuthor.getId());
		}

		return false;
	}
}
