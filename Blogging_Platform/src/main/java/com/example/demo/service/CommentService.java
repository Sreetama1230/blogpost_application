package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.CommentDTO;
import com.example.demo.dto.CommentReact;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvalidReactException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Comment;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private EventDao eventDao;
	@Autowired
	private ObjectMapper objectMapper;

	Logger logger = LoggerFactory.getLogger(Comment.class);

	@Transactional
	public BlogPostResponse createOrUpdateComment(CommentDTO c, long blogPostId) throws JsonProcessingException {

		logger.info("request started processing for " + c.getMessage());
		Comment upsertComment = null;

		Event event = new Event();
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

						upsertComment = commentDao.save(existingComment);

						authDbUser.getComments().add(upsertComment);
						event.setEventType(EventType.UPDATE);

					}

				} else {
					throw new DoNotHavePermissionError("You can not make changes on this comment!");
				}

			}

		} else {
			upsertComment = commentDao.save(newComment);
			exitsingComments.add(newComment);
			authDbUser.getComments().add(upsertComment);
			event.setEventType(EventType.CREATE);
		}

		BlogPost newBlogPostWithComment = blogPostDao.save(dbBlogPost);

		userdao.save(authDbUser);

		event.setCreatedAt(LocalDateTime.now());
		event.setPayload(objectMapper.writeValueAsString(c));
		event.setPublishedAt(LocalDateTime.now());
		event.setStatus(EventStatus.PENDING);
		event.setTransactionId(String.valueOf(upsertComment.getId()));
		event.setTransactionType(TransactionType.COMMENT);
		event.setRetryCount(0);
		eventDao.save(event);

		logger.info("returning response... " + newBlogPostWithComment.getContent());
		return BlogPostResponse.convertBlogPostRespons(newBlogPostWithComment);
	}

	@Transactional
	public CommentResponse deleteComment(long id, long blogpostid) throws JsonProcessingException {

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

				Event event = new Event();
				event.setEventType(EventType.DELETE);
				event.setCreatedAt(LocalDateTime.now());
				event.setPayload(objectMapper.writeValueAsString(id));
				event.setPublishedAt(LocalDateTime.now());
				event.setStatus(EventStatus.PENDING);
				event.setTransactionId(String.valueOf(c.getId()));
				event.setTransactionType(TransactionType.COMMENT);
				event.setRetryCount(0);
				eventDao.save(event);

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
	public CommentResponse reactComment(CommentReact commentReact) throws JsonProcessingException {
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
			Comment reactedComment = commentDao.save(dbComment);

			Event event = new Event();

			event.setEventType(EventType.REACT);
			event.setCreatedAt(LocalDateTime.now());
			event.setPayload(objectMapper.writeValueAsString(commentReact));
			event.setPublishedAt(LocalDateTime.now());
			event.setStatus(EventStatus.PENDING);
			event.setTransactionId(String.valueOf(reactedComment.getId()));
			event.setTransactionType(TransactionType.COMMENT);
			event.setRetryCount(0);
			eventDao.save(event);

			return CommentResponse.convertCommentResponse(reactedComment);
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
