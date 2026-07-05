package com.example.demo.controller.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.constants.AppConstants;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CategoryDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.error.ErrorDetails;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CategoryResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = { AppConstants.ADMINTOOL_TOPIC_NAME })
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

	private static HttpHeaders headers;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@BeforeAll
	public static void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	private String createURLWithPort() {
		return "http://localhost:" + port + "/category";

	}

	@AfterEach
	public void cleanUp() {
		postDao.deleteAll();
		categoryDao.deleteAll();
		userDao.deleteAll();

	}

	@Test
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

		// message is getting consumed by consumer
		Map<String, Object> props = KafkaTestUtils.consumerProps("category-create-test-group", "true",
				embeddedKafkaBroker);
		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new StringDeserializer()).createConsumer();

		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, AppConstants.ADMINTOOL_TOPIC_NAME);
		ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

		CategoryResponse categoryResponse = response.getBody();
		boolean isPresent = StreamSupport.stream(consumerRecords.spliterator(), false)
				.anyMatch(record -> record.value().contains("Created Category " + categoryResponse.getId()));
		assertTrue(isPresent);
		assertNotNull(categoryResponse);
		assertEquals("#fake-category", categoryResponse.getName());

	}

	@Test
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

		HttpEntity<String> entity =

				new HttpEntity<>(null, headers);

		Map<String, Object> props = KafkaTestUtils.consumerProps("category-delete-test-group", "true", embeddedKafkaBroker);

		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new StringDeserializer()).createConsumer();

		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, AppConstants.ADMINTOOL_TOPIC_NAME);

		ResponseEntity<Category> resp = template.exchange(createURLWithPort() + "/" + category.getId(),
				HttpMethod.DELETE, entity, Category.class);

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);

		Category deletedResponse = resp.getBody();

		boolean isPresent = StreamSupport.stream(records.spliterator(), false)
				.anyMatch(record -> record.value().contains("Deleted Category " + category.getId()));

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertTrue(isPresent);
		assertEquals("#fake-category", deletedResponse.getName());
		assertNotNull(deletedResponse);

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
