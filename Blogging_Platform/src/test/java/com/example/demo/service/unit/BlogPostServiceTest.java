package com.example.demo.service.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.CategoryException;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class BlogPostServiceTest {

	@Mock
	private BlogPostDao blogPostDao;
	@Mock
	private UserDao userDao;

	@Mock
	private CommentDao commentDao;
	@Mock
	private UserService userService;
	@Mock
	private CategoryService categoryService;
	
	@Mock
	private EventDao eventDao;
	@Mock
	private ObjectMapper objectMapper;
	
	
	@InjectMocks
	private BlogPostService blogPostService;

	private User user;
	private User user1;
	private BlogPost blogPost;
	private Category category;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setUsername("fake-username");
		user.setEmail("fake-email");
		Set<String> rolesSet = new HashSet<>();
		rolesSet.add("ROLE_ADMIN");
		user.setRoles(rolesSet);

		user1 = new User();
		user1.setId(2L);
		user1.setUsername("fake-username-2");
		user1.setEmail("fake-email-2");

		category = new Category();
		category.setName("fake-category");
		category.setId(1L);

		blogPost = new BlogPost();
		blogPost.setId(1L);
		blogPost.setTitle("fake-title");
		blogPost.setContent("fake-content");
		blogPost.setComments(new ArrayList<>());
		blogPost.setAuthor(user);
		blogPost.setCategories(Set.of(category));

		HashSet<BlogPost> set = new HashSet<>();
		set.add(blogPost);
		category.setBlogPosts(set);
		user.setBlogPosts(new ArrayList<>(List.of(blogPost)));

	}

	@Test
	void testGetAll() {
		when(blogPostDao.findAll()).thenReturn(List.of(blogPost));
		List<BlogPost> bps = blogPostService.getAll();
		assertEquals(1, bps.size());
		assertEquals("fake-title", bps.get(0).getTitle());

	}

	@Test
	void testGetById_Success() {
		when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
		BlogPost post = blogPostService.getById(1L);
		assertEquals("fake-title", post.getTitle());
		assertEquals("fake-content", post.getContent());
	}

	@Test
	void testGetById_WithInvalidId_FailureWithResourceNotFoundException() {
		when(blogPostDao.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class,

				() -> {
					blogPostService.getById(1L);
				});

		assertNotNull(resourceNotFoundException);
		;
	}

	@Test
	void testCreateBlogPost_Success() throws JsonProcessingException {

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle(blogPost.getTitle());
		blogPostDTO.setContent(blogPost.getContent());
		Set<CategoryDTO> dtos = new HashSet<>();
		for (Category c : blogPost.getCategories()) {
			dtos.add(new CategoryDTO(c.getName()));
		}
		blogPostDTO.setCategories(dtos);

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userService.getbyId(1L)).thenReturn(user);

			
			when(blogPostDao.save(any(BlogPost.class))).thenReturn(blogPost);
			BlogPostResponse blogPostResponse = blogPostService.createOrUpdateBlogPost(blogPostDTO);
			assertEquals(blogPost.getContent(), blogPostResponse.getContent());
			assertEquals(blogPost.getTitle(), blogPostResponse.getTitle());
			assertEquals(blogPost.getAuthor().getUsername(), blogPostResponse.getAuthor().getUsername());

		
			String payload = objectMapper.writeValueAsString(blogPostDTO);

			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao , atLeastOnce()).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.CREATE, event.getEventType());
			assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals( "1", event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
		
		}
	}

	@Test
	void testCreateBlogPost_WithEmptyCategory_FailureWithCategoryException() {

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle(blogPost.getTitle());
		blogPostDTO.setContent(blogPost.getContent());

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userService.getbyId(1L)).thenReturn(user);

			CategoryException categoryException = assertThrows(CategoryException.class, () -> {
				blogPostService.createOrUpdateBlogPost(blogPostDTO);
			});

			assertNotNull(categoryException);
		}
	}

	@Test
	void testUpdateBlogPost_Success() throws JsonProcessingException {

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle("new-update-fake-title");
		blogPostDTO.setContent("new-update-fake-content");
		blogPostDTO.setId(1L);
		Set<CategoryDTO> dtos = new HashSet<>();
		for (Category c : blogPost.getCategories()) {
			dtos.add(new CategoryDTO(c.getName()));
		}
		blogPostDTO.setCategories(dtos);

		try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userService.getbyId(1L)).thenReturn(user);
			when(blogPostDao.save(any(BlogPost.class))).thenReturn(blogPost);
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
			when(userDao.findById(blogPost.getId())).thenReturn(Optional.of(user));
			BlogPostResponse blogPostResponse = blogPostService.createOrUpdateBlogPost(blogPostDTO);
			
		
			String payload = objectMapper.writeValueAsString(blogPostDTO);

			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao , atLeastOnce()).save(captor.capture());

			Event event = captor.getValue();
			
			assertEquals("new-update-fake-content", blogPostResponse.getContent());
			assertEquals("new-update-fake-title", blogPostResponse.getTitle());
			assertEquals(blogPost.getAuthor().getUsername(), blogPostResponse.getAuthor().getUsername());

			assertEquals(EventType.UPDATE, event.getEventType());
			assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(blogPostResponse.getId() + "", event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
		
		
		}
	}

	@Test
	void testUpdateBlogPost_WithWrongInvalidId_FailureWithResourceNotFoundException() {

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle("new-fake-title");
		blogPostDTO.setContent("new-fake-content");
		blogPostDTO.setId(1L);
		Set<CategoryDTO> dtos = new HashSet<>();
		for (Category c : blogPost.getCategories()) {
			dtos.add(new CategoryDTO(c.getName()));
		}
		blogPostDTO.setCategories(dtos);

		try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userService.getbyId(1L)).thenThrow(ResourceNotFoundException.class);

			ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
				blogPostService.createOrUpdateBlogPost(blogPostDTO);
			});

			assertNotNull(resourceNotFoundException);
		}
	}

	@Test
	void testUpdateBlogPost_WithWrongUser_FailureWithDoNotHavePermissionError() {

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle("new-fake-title");
		blogPostDTO.setContent("new-fake-content");
		blogPostDTO.setId(1L);
		Set<CategoryDTO> dtos = new HashSet<>();
		for (Category c : blogPost.getCategories()) {
			dtos.add(new CategoryDTO(c.getName()));
		}
		blogPostDTO.setCategories(dtos);

		try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			User authenticatedUser = new User("user - 2 ", "password - 2", "user@gmail.com", new ArrayList<>(),
					new ArrayList<>());
			authenticatedUser.setId(3L);
			authenticatedUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));
			authenticatedUser.setBio("bio - 2");
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(3L);

			when(userService.getbyId(3L)).thenReturn(authenticatedUser);
			when(userDao.findById(3L)).thenReturn(Optional.of(authenticatedUser));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));

			DoNotHavePermissionError doNotHavePermissionError =

					assertThrows(DoNotHavePermissionError.class, () -> {
						blogPostService.createOrUpdateBlogPost(blogPostDTO);
					});

			assertNotNull(doNotHavePermissionError);
			assertEquals("You can not do the update!", doNotHavePermissionError.getMessage());

		}
	}

	@Test
	void testDeleteBlogPost_Success() throws Exception {
		try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));

			BlogPostResponse blogPostResponse = blogPostService.deleteBlogPost(1L);
			assertEquals("fake-title", blogPostResponse.getTitle());
			assertEquals("fake-content", blogPostResponse.getContent());

			
			String payload = objectMapper.writeValueAsString(blogPostResponse.getId());

			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.DELETE, event.getEventType());
			assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(blogPostResponse.getId() + "", event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
		}
	}

	@Test
	void testDeleteBlogPost_WithInvalidId_FailureWithResourceNotFoundException() {
		try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			when(blogPostDao.findById(1L)).thenReturn(Optional.empty());

			ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
				blogPostService.deleteBlogPost(1L);
			});

			assertNotNull(resourceNotFoundException);
			assertEquals("Resource is not found!", resourceNotFoundException.getMessage());
		}
	}

	@Test
	void testDeleteBlogPost_WithInvalidId_FailureWithDoNotHavePermissionError() {
		


		try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			User authenticatedUser = new User("user - 2 ", "password - 2", "user@gmail.com", new ArrayList<>(),
					new ArrayList<>());
			authenticatedUser.setId(3L);
			authenticatedUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));
			authenticatedUser.setBio("bio - 2");
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(3L);


			when(userDao.findById(3L)).thenReturn(Optional.of(authenticatedUser));
			when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));

			DoNotHavePermissionError doNotHavePermissionError =

					assertThrows(DoNotHavePermissionError.class, () -> {
						blogPostService.deleteBlogPost(1L);
					});

			assertNotNull(doNotHavePermissionError);
			assertEquals("You are not the author of this post or an admin!", doNotHavePermissionError.getMessage());

		}
	}

	@Test
	void testGetBlogsByTitleAndUserId_Success() {

		List<BlogPost> blogPosts = new ArrayList<>();
		blogPosts.add(blogPost);
		when(blogPostDao.findByTitleAndAuthor("fake-title", 1L)).thenReturn(blogPosts);

		List<BlogPostResponse> blogPostResponses = blogPostService.getBlogsByTitleAndUserId("fake-title", 1L);

		assertEquals(blogPosts.size(), blogPostResponses.size());
		assertEquals(blogPosts.get(0).getTitle(), blogPostResponses.get(0).getTitle());
		assertEquals(blogPosts.get(0).getContent(), blogPostResponses.get(0).getContent());

	}

}
