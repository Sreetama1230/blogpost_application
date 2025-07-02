package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.BlogPost;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dao.UserDao;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvaildRoleException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.response.UserResponse;

@Service
public class UserService {

	@Autowired
	private UserDao userDao;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private BlogPostDao blogPostDao;


	public User createUser(UserDTO u) {
		User reqUser = new User(u.getUsername(), passwordEncoder.encode(u.getPassword()), u.getEmail());
		reqUser.setBio(u.getBio());
		reqUser.setTotalPosts(0L);
		reqUser.setFollowers(0L);
		reqUser.setFollowing(0L);
		reqUser.setBlogPosts(new ArrayList<>());
		reqUser.setRoles(UserService.convertRoles(u.getRoles()));

		return userDao.save(reqUser);
	}

	public User updateUser( UserDTO userDTO) {
		long id = userDTO.getId();
		if (id > 0 && userDao.findById(id).isPresent()) {


			User targetUser = userDao.findById(id).get();
			User currentUser = userDao.findById(SecurityUtils.getCurrentUserId()).get();
			if(canUpdateOrDelete(currentUser,targetUser)){
				targetUser.setBio(userDTO.getBio());
				targetUser.setUsername(userDTO.getUsername());
				targetUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
				userDao.save(targetUser);
				return targetUser;
			}else{
				throw  new DoNotHavePermissionError("You don't have proper role to update the user!");
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
			throw new ResourceNotFoundException(e.getMessage());
		}
	}

	@Transactional
	public UserResponse deleteUser(long id) {
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
				return resp;
			} else {
				throw new DoNotHavePermissionError("You can not delete the user!");
			}

		} else {
		throw  new ResourceNotFoundException("No value is present with that id!");
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


	public static boolean canUpdateOrDelete(User currentUser , User targetUser){
		// user does not have access to update or delete itself
		if(SecurityUtils.isUser(currentUser)){
			return false;
		}
		// Admin can update/delete another editor or user but can not delete/update another admin
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
