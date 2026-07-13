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

import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CategoryDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.error.ErrorDetails;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BlogPostControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private static HttpHeaders httpHeaders;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private BlogPostDao blogPostDao;
	
	@Autowired
	private EventDao eventDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private UserService userService;

	@Autowired
	private BlogPostService blogpostService;

	@Autowired
	private AuthService authService;

	@Autowired
	private CommentDao commentDao;

	@Autowired
	private CategoryDao categoryDao;

	@BeforeAll
	public static void init() {
		httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
	}

	private String createURLWithPort() {
		return "http://localhost:" + port + "/blog";

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testCreateBlogPost() throws JsonProcessingException {
		UserDTO newUser = new UserDTO();
		newUser.setBio("test3333-bio");
		newUser.setEmail("test3333@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test33333-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		HttpEntity<String> entity = new HttpEntity<String>(objectMapper.writeValueAsString(blogPostDTO), httpHeaders);

        ResponseEntity<BlogPostResponse> resp = testRestTemplate.exchange(
                createURLWithPort(),
                HttpMethod.POST,
                entity,
                BlogPostResponse.class
        );

        BlogPostResponse blogPostResponse = resp.getBody();


		assertEquals(HttpStatus.CREATED, resp.getStatusCode());
		assertNotNull(blogPostResponse);
		assertEquals("Fake Content", blogPostResponse.getContent());
		assertEquals("Fake Title", blogPostResponse.getTitle());

		
		Event event = eventDao.findByTransactionIdAndEventType(blogPostResponse.getId()+"" , EventType.CREATE).get();

		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
		assertEquals(0, event.getRetryCount());
	}

	@Test
	public void testUpdateBlogPost() throws JsonProcessingException {
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
		blogPostDTO.setTitle("Fake Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		BlogPostResponse blogPostResponse = blogpostService.createOrUpdateBlogPost(blogPostDTO);

		blogPostDTO.setId(blogPostResponse.getId());
		blogPostDTO.setContent("updated-fake-content");

		HttpEntity<String> entity = new HttpEntity<String>(objectMapper.writeValueAsString(blogPostDTO), httpHeaders);

		ResponseEntity<BlogPostResponse> resp = testRestTemplate.exchange(createURLWithPort(), HttpMethod.PUT, entity,
				BlogPostResponse.class);


		BlogPostResponse updatedBlogPostResponse = resp.getBody();

		
		assertNotNull(updatedBlogPostResponse);
		assertEquals("updated-fake-content", updatedBlogPostResponse.getContent());
		assertEquals("Fake Title", updatedBlogPostResponse.getTitle());


        Event event = eventDao.findByTransactionIdAndEventType(blogPostResponse.getId()+"" , EventType.UPDATE).get();


        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(TransactionType.BLOGPOST, event.getTransactionType());

        assertEquals(0, event.getRetryCount());
	}

	@Test
	public void testUpdateBlogPostFailureWithDoNotHavePermissionError() throws JsonProcessingException {

		// blogpost owner
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username-1-" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		BlogPostResponse blogPostResponse = blogpostService.createOrUpdateBlogPost(blogPostDTO);

		UserDTO authUser = new UserDTO();
		authUser.setBio("test-bio");
		authUser.setEmail("test123456@gmail.com");
		authUser.setPassword("password123456");
		authUser.setUsername("test-username-2-" + UUID.randomUUID().toString());
		authUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(authUser);
		authRequest = new AuthRequest(authUser.getUsername(), "password123456");

		authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		blogPostDTO.setId(blogPostResponse.getId());
		blogPostDTO.setContent("updated-fake-content");

		HttpEntity<String> entity = new HttpEntity<String>(objectMapper.writeValueAsString(blogPostDTO), httpHeaders);

		ResponseEntity<ErrorDetails> resp = testRestTemplate.exchange(createURLWithPort(), HttpMethod.PUT, entity,
				ErrorDetails.class);

		ErrorDetails errorDetails = resp.getBody();

		assertNotNull(errorDetails);
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
		assertEquals("You can not do the update!", errorDetails.getMsg());

	}

	@Test
	public void testGetBlogsByTitleAndUserId() throws JsonProcessingException {
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

		ResponseEntity<List<BlogPostResponse>> resp = testRestTemplate.exchange(
				createURLWithPort() + "/title/" + blogPostResponse.getTitle() + "/user/" + savedUser.getId(),
				HttpMethod.GET, null, new ParameterizedTypeReference<List<BlogPostResponse>>() {
				});

		BlogPostResponse response = resp.getBody().get(0);
		assertNotNull(resp);
		assertNotNull("Fake & Title", response.getTitle());
		assertNotNull("Fake Content", response.getContent());

	}

	@Test
	public void testDeleteBlogPost() throws JsonProcessingException {
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(newUser);

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

		HttpEntity<String> entity = new HttpEntity<String>(null, httpHeaders);

		ResponseEntity<BlogPostResponse> resp = testRestTemplate.exchange(
				createURLWithPort() + "/" + blogPostResponse.getId(), HttpMethod.DELETE, entity,
				BlogPostResponse.class);

		BlogPostResponse response = resp.getBody();
		assertNotNull(resp);
		assertNotNull("Fake & Title", response.getTitle());
		assertNotNull("Fake Content", response.getContent());


        Event event = eventDao.findByTransactionIdAndEventType(blogPostResponse.getId()+"" , EventType.DELETE).get();


        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(TransactionType.BLOGPOST, event.getTransactionType());

        assertEquals(0, event.getRetryCount());

	}

	@Test
	public void testDeleteBlogPostWithDoNotHavePermissionError() throws JsonProcessingException {

		// blogpost owner
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username-1-" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		BlogPostResponse blogPostResponse = blogpostService.createOrUpdateBlogPost(blogPostDTO);

		UserDTO authUser = new UserDTO();
		authUser.setBio("test-bio");
		authUser.setEmail("test123456@gmail.com");
		authUser.setPassword("password123456");
		authUser.setUsername("test-username-2-" + UUID.randomUUID().toString());
		authUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(authUser);
		authRequest = new AuthRequest(authUser.getUsername(), "password123456");

		authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		HttpEntity<String> httpEntity = new HttpEntity<>(null, httpHeaders);

		ResponseEntity<ErrorDetails> resp = testRestTemplate.exchange(
				createURLWithPort() + "/" + blogPostResponse.getId(), HttpMethod.DELETE, httpEntity,
				ErrorDetails.class);

		ErrorDetails errorDetails = resp.getBody();

		assertNotNull(resp);
		assertNotNull("You are not the author of this post or an admin!", errorDetails.getMsg());
		assertNotNull(blogPostDao.findById(blogPostResponse.getId()).get());

	}

	@Test
	public void testDeleteBlogPostWithResourceNotFoundError() throws JsonProcessingException {

		UserDTO authUser = new UserDTO();
		authUser.setBio("test-bio");
		authUser.setEmail("test123456@gmail.com");
		authUser.setPassword("password123456");
		authUser.setUsername("test-username-2-" + UUID.randomUUID().toString());
		authUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		userService.createUser(authUser);
		AuthRequest authRequest = new AuthRequest(authUser.getUsername(), "password123456");

		AuthResponse authResp = authService.login(authRequest);

		// authenticating the user
		httpHeaders.setBearerAuth(authResp.getToken());

		HttpEntity<String> httpEntity = new HttpEntity<>(null, httpHeaders);

		ResponseEntity<ErrorDetails> resp = testRestTemplate.exchange(createURLWithPort() + "/1", HttpMethod.DELETE,
				httpEntity, ErrorDetails.class);

		ErrorDetails errorDetails = resp.getBody();

		assertNotNull(resp);
		assertNotNull("Resource is not found!", errorDetails.getMsg());

	}
}
