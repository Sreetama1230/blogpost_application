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
import com.example.demo.model.Category;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CategoryResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CategoryControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate template;

	@Autowired
	private BlogPostService blogPostService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserDao userDao;

	@Autowired
	private BlogPostDao postDao;

	@Autowired
	private CategoryDao categoryDao;

	@Autowired
	private AuthService authService;

	@Autowired
	private CategoryService categoryService;

    @Autowired
    private EventDao eventDao;
	private static HttpHeaders headers;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	public static void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	private String createURLWithPort() {
		return "http://localhost:" + port + "/category";

	}


    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testCreateCategory() throws JsonProcessingException {
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@12345gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		User createdUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(authResp.getToken());

		CategoryDTO categoryDTO = new CategoryDTO();
		categoryDTO.setName("fake-category");

		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(categoryDTO), headers);

		ResponseEntity<CategoryResponse> response = template.exchange(createURLWithPort(), HttpMethod.POST, entity,
				CategoryResponse.class);

		CategoryResponse categoryResponse = response.getBody();

		assertNotNull(categoryResponse);
		assertEquals("#fake-category", categoryResponse.getName());

       Event event =  eventDao.findByTransactionIdAndEventType(categoryResponse.getId()+"" , EventType.CREATE).get();
        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(TransactionType.CATEGORY, event.getTransactionType());
	}

    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testGetAll() throws JsonProcessingException {
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@12345gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		User createdUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(authResp.getToken());

		Category category = categoryDao.save(new Category("#fake-category", new HashSet<>()));

		ResponseEntity<List<CategoryResponse>> response = template.exchange(createURLWithPort(), HttpMethod.GET, null,
				new ParameterizedTypeReference<List<CategoryResponse>>() {

				});

		assertEquals(HttpStatus.OK, response.getStatusCode());
		CategoryResponse categoryResponse = response.getBody().get(0);

		assertNotNull(categoryResponse);
		assertEquals("#fake-category", categoryResponse.getName());
	}


    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testListBlogsByCategory() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@12345789gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username-9" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		User createdUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		blogPostService.createOrUpdateBlogPost(blogPostDTO);

		ResponseEntity<List<BlogPostResponse>> response = template.exchange(
				createURLWithPort() + "/name?name=" + categoryDTO.getName(), HttpMethod.GET, null,
				new ParameterizedTypeReference<List<BlogPostResponse>>() {

				});

		assertEquals(HttpStatus.OK, response.getStatusCode());
		Set<CategoryResponse> categoryResponse = response.getBody().get(0).getCategories();
		assertNotNull(categoryResponse);
		assertEquals(1L, categoryResponse.size());
	}

	@Test
	public void testDeleteById() throws JsonProcessingException {
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@12345gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());

		Category category = categoryDao.save(new Category("#fake-category", new HashSet<>()));

		HttpEntity<String> entity = 	new HttpEntity<>(null, headers);

		ResponseEntity<Category> resp = template.exchange(createURLWithPort() + "/" + category.getId(),
				HttpMethod.DELETE, entity, Category.class);

		Category deletedResponse = resp.getBody();

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertEquals("#fake-category", deletedResponse.getName());
		assertNotNull(deletedResponse);

        Event event = eventDao.findByTransactionIdAndEventType(category.getId()+"" , EventType.DELETE).get();

        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(TransactionType.CATEGORY, event.getTransactionType());
        assertEquals(0, event.getRetryCount());
	}

	@Test
	public void testDeleteByIdWithCategoryLinkedToBlogs() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@12345789gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username-9" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake Title");

		CategoryDTO categoryDTO = new CategoryDTO("fake-category");

		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		blogPostService.createOrUpdateBlogPost(blogPostDTO);

		Category category = categoryService.getByName("#" + categoryDTO.getName());

		HttpEntity<String> entity =

				new HttpEntity<>(null, headers);

		ResponseEntity<ErrorDetails> response = template.exchange(createURLWithPort() + "/" + category.getId(),
				HttpMethod.DELETE, entity, ErrorDetails.class);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Some Blogs are linked with this category!..can not be deleted!", response.getBody().getMsg());
	}

	@Test
	public void testDeleteByIdWithResourceNotFoundException() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("testuser@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username-10-" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());

		HttpEntity<String> entity =

				new HttpEntity<>(null, headers);

		ResponseEntity<ErrorDetails> response = template.exchange(createURLWithPort() + "/1" ,
				HttpMethod.DELETE, entity, ErrorDetails.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Category with provided id is not present.", response.getBody().getMsg());
	}
}
