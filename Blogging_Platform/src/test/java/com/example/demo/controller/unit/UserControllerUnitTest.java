package com.example.demo.controller.unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.config.JwtAuthFilter;
import com.example.demo.config.JwtUtils;
import com.example.demo.controller.*;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostDetailsResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerUnitTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	private UserService userService;

	@MockBean
	private JwtUtils jwtUtils;

	@MockBean
	private JwtAuthFilter jwtAuthFilter;

	User user;

	@Autowired
	private ObjectMapper objectMapper;

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
		set.add("fake-role");
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
	public void testGetAllUsers() {
		UserResponse userResponse = UserResponse.convertUserResponse(user);
		userResponse.setId(1L);
		when(userService.getAll()).thenReturn(List.of(userResponse));

		try {
			mockMvc.perform(MockMvcRequestBuilders.get("/users"))

					.andExpect(status().isOk()).andDo(print()).andExpect(jsonPath("$.size()").value(1))

					.andExpect(jsonPath("$[0].id").value(1L))
					.andExpect(jsonPath("$[0].username").value("fake-username"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testGetPostsByUserId() {

		when(userService.getbyId(1L)).thenReturn(user);

		try {
			mockMvc.perform(MockMvcRequestBuilders.get("/users/1/posts")).andExpect(status().isOk()).andDo(print())
					.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.username").value("fake-username"))
					.andExpect(jsonPath("$.blogPosts.size()").value(1L))
					.andExpect(jsonPath("$.blogPosts[0].id").value(1L))
					.andExpect(jsonPath("$.blogPosts[0].title").value("blog title"));
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	@Test
	public void testGetById() {
		when(userService.getbyId(1L)).thenReturn(user);

		try {
			mockMvc.perform(MockMvcRequestBuilders.get("/users/1")).andExpect(status().isOk()).andDo(print())
					.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.username").value("fake-username"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCreateUser() {
		UserDTO dto = new UserDTO();
		dto.setBio("fake-bio");
		dto.setEmail("fake@gmail.com");
		dto.setPassword("fake-password");
		dto.setUsername("fake-username");
		Set<String> roles = new HashSet<>(Set.of("ROLE_ADMIN"));
		dto.setRoles(roles);

		User newUser = new User(dto.getUsername(), dto.getPassword(), dto.getEmail(), new ArrayList<>(),
				new ArrayList<>());
		newUser.setId(2L);
		newUser.setRoles(roles);
		newUser.setBio(dto.getBio());
		when(userService.createUser(any(UserDTO.class))).thenReturn(newUser);

		try {
			mockMvc.perform(MockMvcRequestBuilders.post("/users/register").content(objectMapper.writeValueAsString(dto))
					.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andDo(print())
					.andExpect(jsonPath("$.username").value("fake-username"))
					.andExpect(jsonPath("$.bio").value("fake-bio"));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testUpdateUser() {

		UserDTO dto = new UserDTO();
		dto.setBio("fake-bio");
		dto.setEmail("fake@gmail.com");
		dto.setPassword("fake-password");
		dto.setUsername("fake-updated-username");
		Set<String> roles = new HashSet<>(Set.of("ROLE_ADMIN"));
		dto.setRoles(roles);

		User newUser = new User(dto.getUsername(), dto.getPassword(), dto.getEmail(), new ArrayList<>(),
				new ArrayList<>());
		newUser.setId(2L);
		newUser.setRoles(roles);
		newUser.setBio(dto.getBio());

		when(userService.updateUser(any(UserDTO.class))).thenReturn(newUser);

		try {
			mockMvc.perform(MockMvcRequestBuilders.put("/users").content(objectMapper.writeValueAsString(dto))
					.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andDo(print())
					.andExpect(jsonPath("$.bio").value("fake-bio"))
					.andExpect(jsonPath("$.username").value("fake-updated-username"))
					.andExpect(jsonPath("$.id").value(2L));

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		verify(userService).updateUser(any(UserDTO.class));

	}

	@Test
	public void testDeleteUser() {
		UserResponse userResponse = UserResponse.convertUserResponse(user);
		when(userService.deleteUser(1L)).thenReturn(userResponse);
		try {
			mockMvc.perform(MockMvcRequestBuilders.delete("/users/1")).andExpect(status().isOk()).andDo(print())
					.andExpect(jsonPath("$.username").value("fake-username"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
