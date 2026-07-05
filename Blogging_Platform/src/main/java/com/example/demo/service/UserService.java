package com.example.demo.service;

import java.beans.EventSetDescriptor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.example.demo.config.SecurityUtils;
import com.example.demo.constants.AppConstants;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Event;

import jakarta.transaction.Transactional;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import com.example.demo.dao.UserDao;
import com.example.demo.exception.CustomExceptionHandler;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvaildRoleException;
import com.example.demo.exception.InvalidEmailIdError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;
import com.example.demo.model.User;
import com.example.demo.response.UserResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

	@Autowired
	private UserDao userDao;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private BlogPostDao blogPostDao;

	@Autowired
	private EventDao eventDao;
	@Autowired
	ObjectMapper objectMapper;

	Logger logger = LoggerFactory.getLogger(UserService.class);

	@Transactional
	public User createUser(UserDTO u) throws JsonProcessingException {
		if (!u.getEmail().contains("@")) {
			throw new InvalidEmailIdError("Please enter a valid email id");
		}

		if (u.getRoles().size() > 1) {
			throw new InvaildRoleException(
					"Sorry! you can not provide more than one role for now.. we are working on this feature..than you!");
		}

		User reqUser = new User(u.getUsername(), passwordEncoder.encode(u.getPassword()), u.getEmail());

		reqUser.setBio(u.getBio());
		reqUser.setTotalPosts(0L);
		reqUser.setFollowers(0L);
		reqUser.setFollowing(0L);
		reqUser.setBlogPosts(new ArrayList<>());
		reqUser.setRoles(UserService.convertRoles(u.getRoles()));

		User newUser = userDao.save(reqUser);

		Event event = new Event();

		event.setEventType(EventType.CREATE);
		event.setCreatedAt(LocalDateTime.now());
		event.setPayload(objectMapper.writeValueAsString(u));
		event.setPublishedAt(LocalDateTime.now());
		event.setStatus(EventStatus.PENDING);
		event.setTransactionId(String.valueOf(newUser.getId()));
		event.setTransactionType(TransactionType.USER);
		event.setRetryCount(0);
		eventDao.save(event);

		return newUser;
	}

	@Transactional
	public User updateUser(UserDTO userDTO) throws JsonProcessingException {
		long id = userDTO.getId();
		if (id > 0 && userDao.findById(id).isPresent()) {

			User targetUser = userDao.findById(id).get();
			User currentUser = userDao.findById(SecurityUtils.getCurrentUserId()).get();

			if (canUpdateOrDelete(currentUser, targetUser)) {

				if (userDTO.getBio() != null) {
					targetUser.setBio(userDTO.getBio());
				}

				if (userDTO.getPassword() != null) {
					targetUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
				}

				if (userDTO.getEmail() != null) {
					if (!userDTO.getEmail().equals(targetUser.getEmail())) {
						throw new UnexpectedCustomException("You can not change the registed email id. ");

					}
				}

				if (userDTO.getUsername() != null) {
					targetUser.setUsername(userDTO.getUsername());
				}

				if (userDTO.getRoles() != null || userDTO.getRoles().size() > 0) {

					if (userDTO.getRoles().size() > 1) {
						throw new InvaildRoleException(
								"Sorry! you can not provide more than one role for now.. we are working on this feature..than you!");
					}

					for (String role : userDTO.getRoles()) {
						if (!(targetUser.getRoles().contains(role))) {
							throw new UnexpectedCustomException("You can not change the role!");
						}
					}
				}

				// email can not be changed
				User updatedUser = userDao.save(targetUser);

				Event event = new Event();

				event.setEventType(EventType.UPDATE);
				event.setCreatedAt(LocalDateTime.now());
				event.setPayload(objectMapper.writeValueAsString(userDTO));
				event.setPublishedAt(LocalDateTime.now());
				event.setStatus(EventStatus.PENDING);
				event.setTransactionId(String.valueOf(updatedUser.getId()));
				event.setTransactionType(TransactionType.USER);
				event.setRetryCount(0);
				eventDao.save(event);

				return updatedUser;
			} else {
				throw new DoNotHavePermissionError("You don't have proper role to update the user!");
			}
		} else {
			throw new ResourceNotFoundException("No user present with the provided id");
		}
	}

	public List<UserResponse> getAll() {
		List<User> lists = userDao.findAll();
		List<UserResponse> userResponses = new ArrayList<>();
		for (User u : lists) {
			userResponses.add(UserResponse.convertUserResponse(u));
		}
		return userResponses;
	}

	public User getbyId(long id) {
		try {
			return userDao.findById(id).get();

		} catch (Exception e) {
			throw new ResourceNotFoundException("No user present with the provided id");
		}
	}

	@Transactional
	public UserResponse deleteUser(long id) throws JsonProcessingException {
		// delete a user means deleting all the blog posts attached to that user

		if (userDao.findById(id).isPresent()) {

			User dbUser = userDao.findById(id).get();
			User dbCurUser = userDao.findById(SecurityUtils.getCurrentUserId()).get();
			if (UserService.canUpdateOrDelete(dbCurUser, dbUser)) {
				if (!(dbUser.getBlogPosts().isEmpty())) {
					for (BlogPost b : dbUser.getBlogPosts()) {
						blogPostDao.deleteById(b.getId());
					}
				}
				UserResponse resp = UserResponse.convertUserResponse(dbUser);
				userDao.deleteById(id);

				Event event = new Event();

				event.setEventType(EventType.DELETE);
				event.setCreatedAt(LocalDateTime.now());
				event.setPayload(objectMapper.writeValueAsString(id));
				event.setPublishedAt(LocalDateTime.now());
				event.setStatus(EventStatus.PENDING);
				event.setTransactionId(String.valueOf(dbUser.getId()));
				event.setTransactionType(TransactionType.USER);
				event.setRetryCount(0);
				eventDao.save(event);

				return resp;
			} else {
				throw new DoNotHavePermissionError("You can not delete the user!");
			}

		} else {
			throw new ResourceNotFoundException("No value is present with that id!");
		}
	}

	public static Set<String> convertRoles(Set<String> inputRoles) {
		Set<String> roles = new HashSet<>();
		for (String s : inputRoles) {
			if (s.equalsIgnoreCase("USER") || s.equalsIgnoreCase("ROLE_USER")) {
				roles.add("ROLE_USER");
			} else {
				if (s.equalsIgnoreCase("ADMIN") || s.equalsIgnoreCase("ROLE_ADMIN")) {
					roles.add("ROLE_ADMIN");
				} else {
					if (s.equalsIgnoreCase("EDITOR") || s.equalsIgnoreCase("ROLE_EDITOR")) {
						roles.add("ROLE_EDITOR");
					} else {
						throw new InvaildRoleException("Select a proper role : user,editor or admin");
					}
				}
			}
		}

		return roles;

	}

	public static boolean canUpdateOrDelete(User currentUser, User targetUser) {
		// user does not have access to update or delete itself
		if (SecurityUtils.isUser(currentUser)) {
			return false;
		}
		// Admin can update/delete another editor or user but can not delete/update
		// another admin
		if (SecurityUtils.isAdmin(currentUser)) {
			return currentUser.getId().equals(targetUser.getId()) || SecurityUtils.isEditor(targetUser)
					|| SecurityUtils.isUser(targetUser);
		}

		// Editors can only update/delete themselves
		if (SecurityUtils.isEditor(currentUser)) {
			return currentUser.getId().equals(targetUser.getId());
		}

		return false;
	}

}
