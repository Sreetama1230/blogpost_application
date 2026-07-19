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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvalidEmailIdError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.UserResponse;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
public class UserServiceUnitTest {

	@Mock
	UserDao userDao;

	@Mock
	BlogPostDao blogPostDao;

	@InjectMocks
	UserService userService;
	@Mock
	PasswordEncoder passwordEncoder;

	@Mock
	private EventDao eventDao;
	@Mock
	ObjectMapper objectMapper;

	User user;

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
		BlogPost blogPost = new BlogPost();
		blogPost.setId(1L);
		blogPost.setContent("blog content");
		blogPost.setTitle("blog title");
		blogPost.setAuthor(user);
		Set<Category> categories = new HashSet<>();
		HashSet<BlogPost> blogPosts = new HashSet<>();
		blogPosts.add(blogPost);
		categories.add(new Category("fake-category", blogPosts));
		blogPost.setCategories(categories);

		user.setBlogPosts(List.of(blogPost));

	}

	@Test
	public void testCreateUser_Success() throws JsonProcessingException {
		UserDTO userDTO = new UserDTO();
		userDTO.setBio(user.getBio());
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail(user.getEmail());
		userDTO.setPassword("fake-password");
		userDTO.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		when(userDao.save(any(User.class))).thenReturn(user);

		User newUser = userService.createUser(userDTO);

		String payload = objectMapper.writeValueAsString(userDTO);
		
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		verify(eventDao).save(captor.capture());

		Event event = captor.getValue();

		assertEquals(EventType.CREATE, event.getEventType());
		assertEquals(TransactionType.USER, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(newUser.getId()+"" ,  event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());
		
		assertEquals(user.getBio(), newUser.getBio());
		assertEquals(user.getId(), newUser.getId());
		verify(userDao).save(user);
	}

	@Test
	public void testCreateUser_WithInvalidEmail_FailureWithInvalidEmailIdError() {
		UserDTO userDTO = new UserDTO();
		userDTO.setBio(user.getBio());
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail("fake-email");
		userDTO.setPassword("fake-password");
		userDTO.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		InvalidEmailIdError invalidEmailIdError = assertThrows(InvalidEmailIdError.class, () -> {

			userService.createUser(userDTO);
		});

		assertNotNull(invalidEmailIdError);
		assertEquals("Please enter a valid email id", invalidEmailIdError.getMessage());

	}

	@Test
	public void testUpdateUser_Success() throws JsonProcessingException {

		UserDTO userDTO = new UserDTO();
		// update
		userDTO.setId(1L);
		userDTO.setBio("new-updated-bio");
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail(user.getEmail());
		userDTO.setPassword("fake-password");
		userDTO.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class, Mockito.CALLS_REAL_METHODS)) {

			mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userDao.findById(1L)).thenReturn(Optional.of(user));

			when(userDao.save(any(User.class))).thenReturn(user);

			User newUser = userService.updateUser(userDTO);
			
			
			String payload = objectMapper.writeValueAsString(userDTO);
			
			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.UPDATE, event.getEventType());
			assertEquals(TransactionType.USER, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(newUser.getId()+"" ,  event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
			

			assertEquals("new-updated-bio", newUser.getBio());
			assertEquals(user.getId(), newUser.getId());
			assertEquals(user.getEmail(), newUser.getEmail());

			verify(userDao).save(user);

		}

	}

	@Test
	public void testUpdateUser_WithInvalidId_FailureWithResourceNotFoundException() {

		UserDTO userDTO = new UserDTO();
		// update
		userDTO.setId(1L);
		userDTO.setBio("new-updated-bio");
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail(user.getEmail());
		userDTO.setPassword("fake-password");
		userDTO.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class, Mockito.CALLS_REAL_METHODS)) {

			mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			when(userDao.findById(1L)).thenReturn(Optional.empty());

			ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
				userService.updateUser(userDTO);
			});

			assertNotNull(resourceNotFoundException);
			assertEquals("No user present with the provided id", resourceNotFoundException.getMessage());

		}

	}

	@Test
	public void testUpdateUser_WithWrongUser_FailureWithDoNotHavePermissionError() {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			User authenticatedUser = new User("user - 2 ", "password - 2", "user@gmail.com", new ArrayList<>(),
					new ArrayList<>());
			authenticatedUser.setId(3L);
			authenticatedUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));
			authenticatedUser.setBio("bio - 2");
			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(3L);

			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(userDao.findById(3L)).thenReturn(Optional.of(authenticatedUser));

			UserDTO userDTO = new UserDTO();
			// update
			userDTO.setId(1L);
			userDTO.setBio("new-updated-bio");
			userDTO.setUsername(user.getUsername());
			userDTO.setEmail(user.getEmail());
			userDTO.setPassword("fake-password");
			userDTO.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

			DoNotHavePermissionError doNotHavePermissionError = assertThrows(DoNotHavePermissionError.class, () -> {
				userService.updateUser(userDTO);
			});

			assertNotNull(doNotHavePermissionError);
			assertEquals("You don't have proper role to update the user!", doNotHavePermissionError.getMessage());

		}

	}

	@Test
	public void testGetAll__Success() {

		when(userDao.findAll()).thenReturn(List.of(user));

		List<UserResponse> resp = userService.getAll();

		assertEquals(1L, resp.get(0).getId());
		assertEquals(user.getBio(), resp.get(0).getBio());
		assertEquals(user.getEmail(), resp.get(0).getEmail());
		verify(userDao).findAll();
	}

	@Test
	public void testGetById__Success() {
		when(userDao.findById(1L)).thenReturn(Optional.of(user));

		User fetchedUser = userService.getbyId(1L);

		assertEquals(user.getBio(), fetchedUser.getBio());
		assertEquals(user.getId(), fetchedUser.getId());
		assertEquals(user.getEmail(), fetchedUser.getEmail());
		assertEquals(user.getFollowers(), fetchedUser.getFollowers());
		assertEquals(user.getFollowing(), fetchedUser.getFollowing());

		verify(userDao).findById(1L);

	}

	@Test
	public void testGetById__WithInvalidId_FailureWithResourceNotFoundException() {
		when(userDao.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			userService.getbyId(1L);
		});

		assertNotNull(resourceNotFoundException);
		assertEquals("No user present with the provided id", resourceNotFoundException.getMessage());
	}

	@Test
	public void testDeleteUser__Success() throws JsonProcessingException {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			when(userDao.findById(1L)).thenReturn(Optional.of(user));

			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

			UserResponse userResponse = userService.deleteUser(1L);

			String payload = objectMapper.writeValueAsString(user.getId());
			
			ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

			verify(eventDao).save(captor.capture());

			Event event = captor.getValue();

			assertEquals(EventType.DELETE, event.getEventType());
			assertEquals(TransactionType.USER, event.getTransactionType());
			assertEquals(EventStatus.PENDING, event.getStatus());
			assertEquals(user.getId()+"" ,  event.getTransactionId());
			assertEquals(0, event.getRetryCount());
			assertEquals(payload, event.getPayload());
			
			
			assertEquals(user.getBio(), userResponse.getBio());

			assertEquals(1L, userResponse.getId());

			verify(userDao).deleteById(1L);

		}
	}

	@Test
	public void testDeleteUser__WithInvalidId_FailureWithResourceNotFoundException() {

		when(userDao.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			userService.deleteUser(1L);
		});

		assertNotNull(resourceNotFoundException);
		assertEquals("No value is present with that id!", resourceNotFoundException.getMessage());

	}

	@Test
	public void testDeleteUser_WithWrongUser_FailureWithDoNotHavePermissionError() {

		try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class,
				Mockito.CALLS_REAL_METHODS)) {

			User authenticatedUser = new User("user - 2 ", "password - 2", "user@gmail.com", new ArrayList<>(),
					new ArrayList<>());
			authenticatedUser.setId(3L);
			authenticatedUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));
			authenticatedUser.setBio("bio - 2");
			mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(3L);

			when(userDao.findById(1L)).thenReturn(Optional.of(user));
			when(userDao.findById(3L)).thenReturn(Optional.of(authenticatedUser));

			DoNotHavePermissionError doNotHavePermissionError = assertThrows(DoNotHavePermissionError.class, () -> {
				userService.deleteUser(1L);
			});

			assertNotNull(doNotHavePermissionError);
			assertEquals("You can not delete the user!", doNotHavePermissionError.getMessage());

		}

	}

}
