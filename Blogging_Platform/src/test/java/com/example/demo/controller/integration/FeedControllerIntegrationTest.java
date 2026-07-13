package com.example.demo.controller.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.FeedItem;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FeedControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private static HttpHeaders httpHeaders;

	@Autowired
	private UserService userService;

	@Autowired
	private BlogPostService blogpostService;

	@Autowired
	private AuthService authService;

	@BeforeAll
	public static void init() {
		httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
	}

	private String createURLWithPort() {
		return "http://localhost:" + port + "/timeline";

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testTimeline_LoggedOut() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User savedUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake & Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		BlogPostResponse blogPostResponse = blogpostService.createOrUpdateBlogPost(blogPostDTO);

		ResponseEntity<List<FeedItem>> resp = testRestTemplate.exchange(createURLWithPort() + "?start=0&size=10",
				HttpMethod.GET, null, new ParameterizedTypeReference<List<FeedItem>>() {
				});

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		FeedItem response = resp.getBody().get(0);
		assertEquals("Fake & Title", response.getBlogDescription());
		assertNotNull(resp);

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testTimeline_LoggedIn() throws JsonProcessingException {

		UserDTO newUser1 = new UserDTO();
		newUser1.setBio("test-bio");
		newUser1.setEmail("test2222@gmail.com");
		newUser1.setPassword("password123");
		newUser1.setUsername("test-username" + UUID.randomUUID().toString());
		newUser1.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User savedUser1 = userService.createUser(newUser1);

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User savedUser = userService.createUser(newUser);

		savedUser1.setListfollowers(Set.of(savedUser));
		savedUser1.setFollowers(1L);

		savedUser.setListfollowing(Set.of(savedUser1));
		savedUser.setFollowing(1L);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake & Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		BlogPostResponse blogPostResponse = blogpostService.createOrUpdateBlogPost(blogPostDTO);

		AuthRequest authRequest1 = new AuthRequest(newUser1.getUsername(), "password123");

		AuthResponse authResp1 = authService.login(authRequest1);

		HttpHeaders headers1 = new HttpHeaders();
		headers1.setContentType(MediaType.APPLICATION_JSON);
		headers1.setBearerAuth(authResp1.getToken());

		HttpEntity<String> entity = new HttpEntity<>(null, headers1);

		ResponseEntity<List<FeedItem>> resp = testRestTemplate.exchange(createURLWithPort() + "?start=0&size=10",
				HttpMethod.GET, entity, new ParameterizedTypeReference<List<FeedItem>>() {
				});

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		FeedItem response = resp.getBody().get(0);
		assertNotNull(resp);
		assertEquals("Fake & Title", response.getBlogDescription());

	}

}
