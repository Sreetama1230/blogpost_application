package com.example.demo.service;

import java.util.*;

import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
	
	@Mock
	private UserDao userDao;
	
	@Mock
	private BlogPostDao blogPostDao;
	@Mock
	private PasswordEncoder passwordEncoder;
	
	@InjectMocks
	private UserService userService;
	
	private User user;
	private UserDTO dto;
	private UserDTO dto1;
	private User targetUser;
	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setUsername("fake-username");
		user.setPassword("fake-password");
		user.setBio("fake-bio");
		user.setEmail("fake-email");
		user.setFollowers(1L);
		user.setFollowing(1L);
		user.setBlogPosts(new ArrayList<>());
		user.setTotalPosts(0L);

		targetUser = new User();
		targetUser .setId(3L);
		targetUser.setUsername("fake-username-3");
		targetUser.setPassword("fake-password-3");
		targetUser .setBio("fake-bio-3");
		targetUser .setEmail("fake-email-3");
		targetUser .setFollowers(1L);
		targetUser .setFollowing(1L);
		targetUser .setBlogPosts(new ArrayList<>());
		targetUser .setTotalPosts(0L);


		dto = new UserDTO();
		dto.setId(1L);
		dto.setBio("fake-new-bio");
		dto.setEmail("fake-new-email");
		dto.setPassword("fake-new-password");
		dto.setUsername("fake-username-updated");


		dto1 = new UserDTO();
		dto1.setId(3L);
		dto1.setBio("fake-new-bio-3");
		dto1.setEmail("fake-new-email-3");
		dto1.setPassword("fake-new-password-3");
		dto1.setUsername("fake-username-updated-3");
	}
	
		   @Test
		    void testCreateOrUpdateUser() {
		     when(userDao.save(any(User.class))).thenReturn(user);
			 dto.setPassword(passwordEncoder.encode(dto.getPassword()));
		     User savedUser = userService.createUser(dto);
		     assertEquals("fake-username", savedUser.getUsername());
		   }

		   @Test
		   void testEditorUpdateUser_Success() {
			   when(userDao.findById(1L)).thenReturn(Optional.of(user));
			   try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
				   utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
				   utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
				   utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
				   utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
				   mockStatic(UserService.class).when(()-> UserService.canUpdateOrDelete(user,user)).thenReturn(true);
				   User updated = userService.updateUser(dto);
				   assertEquals("fake-username-updated", updated.getUsername());

			   }

		   }

	@Test
	void testAdminUpdateUser_Success() {
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		when(userDao.findById(3L)).thenReturn(Optional.of(targetUser));
		try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(true);

			utilities.when(()->SecurityUtils.isEditor(targetUser)).thenReturn(true);
			utilities.when(()->SecurityUtils.isAdmin(targetUser)).thenReturn(false);
			utilities.when(()->SecurityUtils.isUser(targetUser)).thenReturn(false);
			mockStatic(UserService.class).when(()-> UserService.canUpdateOrDelete(user,targetUser)).thenReturn(true);
			User updated = userService.updateUser(dto1);
			assertEquals("fake-username-updated-3", updated.getUsername());

		}

	}

	@Test
	void testUpdateUser_Failure() {
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		when(userDao.findById(3L)).thenReturn(Optional.of(targetUser));
		try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(true);
			utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);

			utilities.when(()->SecurityUtils.isEditor(targetUser)).thenReturn(true);
			utilities.when(()->SecurityUtils.isAdmin(targetUser)).thenReturn(false);
			utilities.when(()->SecurityUtils.isUser(targetUser)).thenReturn(false);
			mockStatic(UserService.class).when(()-> UserService.canUpdateOrDelete(user,targetUser)).thenReturn(false);

			assertThrows(DoNotHavePermissionError.class,()-> userService.updateUser(dto1));


		}

	}
		@Test
		   void testUpdateUser_NotFound(){
		     dto.setId(2L);
			 when(userDao.findById(2L)).thenReturn(Optional.empty());
			 assertThrows(ResourceNotFoundException.class,()->userService.updateUser(dto));

		   }

		   @Test
		   void testGetAllUsers(){

		    List<User> mockUsers = Arrays.asList(user);
			when(userDao.findAll()).thenReturn(mockUsers);

			List<UserResponse> result = userService.getAll();
			assertEquals(1,result.size());
		    assertEquals("fake-username",result.get(0).getUsername());


		   }

		   @Test
		   void testGetById_Success(){
		     when(userDao.findById(1L)).thenReturn( Optional.of(user));
			 User fetched = userService.getbyId(1L);
			 assertEquals("fake-username",fetched.getUsername());
		   }
          @Test
		   void testGetById_NotFound(){
		    when(userDao.findById(99L)).thenThrow(new NoSuchElementException("Not Found"));
			assertThrows(ResourceNotFoundException.class,()-> userService.getbyId(99L));
		   }
	@Test
	void testEditorDeleteUser_Success() {
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
			utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
			mockStatic(UserService.class).when(()-> UserService.canUpdateOrDelete(user,user)).thenReturn(true);
			UserResponse deleted = userService.deleteUser(1L);
			assertEquals("fake-username", deleted.getUsername());

		}

	}

	@Test
	void testAdminDeleteUser_Success() {
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		when(userDao.findById(3L)).thenReturn(Optional.of(targetUser));
		try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(true);

			utilities.when(()->SecurityUtils.isEditor(targetUser)).thenReturn(true);
			utilities.when(()->SecurityUtils.isAdmin(targetUser)).thenReturn(false);
			utilities.when(()->SecurityUtils.isUser(targetUser)).thenReturn(false);
			mockStatic(UserService.class).when(()-> UserService.canUpdateOrDelete(user,targetUser)).thenReturn(true);
			UserResponse deleted = userService.deleteUser(3L);
			assertEquals("fake-username-3", deleted.getUsername());

		}

	}

	@Test
	void testDeleteUser_Failure() {
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		when(userDao.findById(3L)).thenReturn(Optional.of(targetUser));
		try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
			utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
			utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
			utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
			utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);

			utilities.when(()->SecurityUtils.isEditor(targetUser)).thenReturn(true);
			utilities.when(()->SecurityUtils.isAdmin(targetUser)).thenReturn(false);
			utilities.when(()->SecurityUtils.isUser(targetUser)).thenReturn(false);
			mockStatic(UserService.class).when(()-> UserService.canUpdateOrDelete(user,targetUser)).thenReturn(false);

			assertThrows(DoNotHavePermissionError.class,()-> userService.deleteUser(3L));


		}

	}






}
