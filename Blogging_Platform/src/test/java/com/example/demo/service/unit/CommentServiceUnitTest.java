package com.example.demo.service.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.CommentDTO;
import com.example.demo.dto.CommentReact;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.Reaction;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvalidReactException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;
import com.example.demo.service.CommentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
public class CommentServiceUnitTest {

	@InjectMocks
	CommentService commentService;

	@Mock
	CommentDao commentDao;

	@Mock
	private BlogPostDao blogPostDao;

	@Mock
	private UserDao userDao;

	@Mock
	private EventDao eventDao;
	@Mock
	private ObjectMapper objectMapper;
	
	
	User user;
	BlogPost blogPost;
	Comment comment;
	Set<Category> categories;
	List<Category> listCategory = new ArrayList<>();

	@BeforeEach
	void setUp() {

		user = new User();
		user.setUsername("fake-username");
		user.setBio("fake-bio");
		user.setEmail("fake@ppp.com");
		user.setId(1L);
		user.setFollowing(0L);
		user.setTotalPosts(0L);
		user.setFollowing(0L);
		user.setFollowers(0L);
		user.setPassword("fake-password");
		Set<String> set = new HashSet<>();
		set.add("ROLE_ADMIN");
		user.setRoles(set);
		blogPost = new BlogPost();
		blogPost.setId(1L);
		blogPost.setContent("blog content");
		blogPost.setTitle("blog title");
		blogPost.setAuthor(user);
		categories = new HashSet<>();
		HashSet<BlogPost> blogPosts = new HashSet<>();
		blogPosts.add(blogPost);
		categories.add(new Category("fake-category", blogPosts));
		blogPost.setCategories(categories);

		comment = new Comment();
		comment.setId(1L);
		comment.setContent("test comment");
		comment.setLikeCount(1L);
		comment.setFunnyCount(1L);
		comment.setLoveCount(1L);
		comment.setUser(user);
		blogPost.setComments(new ArrayList<>(List.of(comment)));
		comment.setBlogPost(blogPost);

		List<BlogPost> listofUserBlogPosts = new ArrayList<>();
		listofUserBlogPosts.add(blogPost);
		user.setBlogPosts(listofUserBlogPosts);

		for (Category category : categories) {
			listCategory.add(category);
		}

	}

	@Test
	public void testCreateComment() throws JsonProcessingException {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			CommentDTO newComment = new CommentDTO();
			newComment.setMessage("test comment 2");

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(commentDao.save(any(Comment.class))).thenReturn(comment);
			when(blogPostDao.save(blogPost)).thenReturn(blogPost);


			BlogPostResponse blogPostResponse = commentService.createOrUpdateComment(newComment, 1L);

			assertEquals(comment.getId(), blogPostResponse.getComments().get(0).getId());
			assertEquals(comment.getContent(), blogPostResponse.getComments().get(0).getContent());
			
			
			String payload = objectMapper.writeValueAsString(newComment);
			
			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.CREATE, event.getEventType());
			assertEquals(TransactionType.COMMENT, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(blogPostResponse.getId()+"" ,  event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());

		}

	}

	@Test
	public void testUpdateComment() throws JsonProcessingException {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			CommentDTO newComment = new CommentDTO();
			newComment.setMessage("test comment 2");
			newComment.setCommentId(1L);

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(commentDao.save(any(Comment.class))).thenReturn(comment);
			when(blogPostDao.save(blogPost)).thenReturn(blogPost);

			BlogPostResponse blogPostResponse = commentService.createOrUpdateComment(newComment, 1L);

			assertEquals(comment.getId(), blogPostResponse.getComments().get(0).getId());
			assertEquals(newComment.getMessage() + "(edited)", blogPostResponse.getComments().get(0).getContent());
			assertEquals(comment.getContent(), blogPostResponse.getComments().get(0).getContent());

		

			String payload = objectMapper.writeValueAsString(newComment);
			
			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.UPDATE, event.getEventType());
			assertEquals(TransactionType.COMMENT, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(blogPostResponse.getId()+"" ,  event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
		
		
		}

	}

	@Test
	public void testUpdateComment_WithInvalidCommentId_FailureWithResourceNotFoundException() {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			CommentDTO newComment = new CommentDTO();
			newComment.setMessage("test comment 2");
			newComment.setCommentId(1L);

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(commentDao.findById(1L)).thenReturn(Optional.empty());

			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(commentDao.save(any(Comment.class))).thenReturn(comment);
			when(blogPostDao.save(blogPost)).thenReturn(blogPost);


			ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
				commentService.createOrUpdateComment(newComment, 1L);
			});

			assertNotNull(resourceNotFoundException);
			assertEquals("No comment is present with the provided id...", resourceNotFoundException.getMessage());

		}

	}

	@Test
	public void testUpdateComment_WithInvalidCommentId_FailureWithDoNotHavePermissionError() {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			CommentDTO newComment = new CommentDTO();
			newComment.setMessage("test comment 2");
			newComment.setCommentId(1L);

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			// current authenticated user
			User authenticatedUser = new User("user - 2 ", "password - 2", "user@gmail.com", new ArrayList<>(),
					new ArrayList<>());
			authenticatedUser.setId(3L);
			authenticatedUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));
			authenticatedUser.setBio("bio - 2");
			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(3L);

			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(userDao.findById(3L)).thenReturn(Optional.of(authenticatedUser));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(commentDao.findById(1L)).thenReturn(Optional.of(comment));

			DoNotHavePermissionError doNotHavePermissionError = assertThrows(DoNotHavePermissionError.class, () -> {
				commentService.createOrUpdateComment(newComment, 1L);
			});

			assertNotNull(doNotHavePermissionError);
			assertEquals("You can not make changes on this comment!", doNotHavePermissionError.getMessage());

		}

	}

	@Test
	public void testDeleteComment() throws JsonProcessingException {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
			when(commentDao.findCommentByIdAndBlogPostId(1L, 1L)).thenReturn(Optional.of(comment));
			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));

			CommentResponse commentResponse = commentService.deleteComment(1L, 1L);

			assertEquals("test comment", commentResponse.getContent());
			

			String payload = objectMapper.writeValueAsString(commentResponse.getId());
			
			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.DELETE, event.getEventType());
			assertEquals(TransactionType.COMMENT, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(commentResponse.getId()+"" ,  event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());

		}

	}

	@Test
	public void testDeleteComment_WithWrongUser_FailureWithDoNotHavePermissionError() {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			CommentDTO newComment = new CommentDTO();
			newComment.setMessage("test comment 2");
			newComment.setCommentId(1L);

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			// current authenticated user
			User authenticatedUser = new User("user - 2 ", "password - 2", "user@gmail.com", new ArrayList<>(),
					new ArrayList<>());
			authenticatedUser.setId(3L);
			authenticatedUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));
			authenticatedUser.setBio("bio - 2");
			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(3L);

			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(userDao.findById(3L)).thenReturn(Optional.of(authenticatedUser));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
			when(commentDao.findCommentByIdAndBlogPostId(1L, 1L)).thenReturn(Optional.of(comment));


			DoNotHavePermissionError doNotHavePermissionError = assertThrows(DoNotHavePermissionError.class, () -> {
				commentService.deleteComment(1L, 1L);
			});

			assertNotNull(doNotHavePermissionError);
			assertEquals("You are not allowed to change other's comment!", doNotHavePermissionError.getMessage());

		}

	}

	@Test
	public void testDeleteComment_WithInvalidCommentId_FailureWithResourceNotFoundException() {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(commentDao.findCommentByIdAndBlogPostId(1L, 1L)).thenReturn(Optional.empty());

			ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
				commentService.deleteComment(1L, 1L);
			});

			assertNotNull(resourceNotFoundException);
			assertEquals("Resource is not present...", resourceNotFoundException.getMessage());

		}

	}

	@Test
	public void testGetById() {
		when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
		CommentResponse commentResponse = commentService.getById(1L);

		assertEquals(comment.getContent(), commentResponse.getContent());
		assertEquals(comment.getId(), commentResponse.getId());
	}

	@Test
	public void testGetById_WithInvalidId_WithResourceNotFoundException() {
		when(commentDao.findById(1L)).thenReturn(Optional.empty());
		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			commentService.getById(1L);
		});

		assertNotNull(resourceNotFoundException);
		assertEquals("Resource is not present!", resourceNotFoundException.getMessage());

	}

	@Test
	public void testCommentReact() throws JsonProcessingException {
		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
			comment.setReactedUsers(new HashSet<>());
		
			
			when(commentDao.save(comment)).thenReturn(comment);
			CommentReact commentReact = new CommentReact(1L, Reaction.LOVE);
			CommentResponse commentResponse = commentService.reactComment(commentReact);
			String payload = objectMapper.writeValueAsString(commentReact);
			
			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.REACT, event.getEventType());
			assertEquals(TransactionType.COMMENT, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(commentResponse.getId()+"" ,  event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
			
			assertEquals(comment.getFunnyCount(), commentResponse.getFunnyCount());
			assertEquals(comment.getLikeCount(), commentResponse.getLikeCount());
			assertEquals(comment.getLoveCount(), commentResponse.getLoveCount());
		}
	}

	@Test
	public void testCommentReact_FailureWithInvalidReactException() {
		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			comment.setReactedUsers(new HashSet<>(List.of(user)));

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
			when(commentDao.save(comment)).thenReturn(comment);

			CommentReact commentReact = new CommentReact(1L, Reaction.LOVE);
			InvalidReactException invalidReactException = assertThrows(InvalidReactException.class, () -> {
				commentService.reactComment(commentReact);
			});

			assertNotNull(invalidReactException);
			assertEquals("You have already reacted!", invalidReactException.getMessage());

		}
	}

}
