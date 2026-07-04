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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.jdbc.Sql;

import com.example.demo.config.SecurityUtils;
import com.example.demo.constants.AppConstants;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.error.ErrorDetails;
import com.example.demo.exception.InvalidEmailIdError;
import com.example.demo.model.User;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostDetailsResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = { AppConstants.ADMINTOOL_TOPIC_NAME })
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

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
	private AuthService authService;

	private  HttpHeaders headers;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@BeforeEach
	public  void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	private String createURLWithPort() {
		return "http://localhost:" + port + "/user";

	}


	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testCreate() throws JsonProcessingException {

		
		UserDTO newUser = new UserDTO();
		newUser.setBio("mytest-bio");
		newUser.setEmail("mytest444@gmail.com");
		newUser.setPassword("mypassword123");
		newUser.setUsername("my-test-username-444" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		HttpEntity<String> entity =

				new HttpEntity<>(objectMapper.writeValueAsString(newUser), headers);

		ResponseEntity<UserResponse> resp = template.exchange(createURLWithPort() + "/register", HttpMethod.POST,
				entity, UserResponse.class);

		Map<String, Object> props = KafkaTestUtils.consumerProps("test-create-user-group", "true", embeddedKafkaBroker);

		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new StringDeserializer()).createConsumer();

		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, AppConstants.ADMINTOOL_TOPIC_NAME);

		UserResponse createdUser = resp.getBody();
	
		assertEquals(HttpStatus.CREATED, resp.getStatusCode());
		
		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);

		boolean isPresent = StreamSupport.stream(records.spliterator(), false)
				.anyMatch(record -> record.value().contains("Created User " + createdUser.getId()));

		assertTrue(isPresent);
		assertNotNull(createdUser);
		assertEquals(newUser.getUsername(), createdUser.getUsername());
		assertEquals(newUser.getEmail(), createdUser.getEmail());

	}


	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testCreateWithInvalidMailId() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("testgmail.com"); // Invalid email
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(newUser), headers);

		ResponseEntity<ErrorDetails> response = template.exchange(createURLWithPort() + "/register", HttpMethod.POST,
				entity, ErrorDetails.class);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull("Please enter a valid email id", response.getBody().getMsg());

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testGetPostsByUserId() {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test1-bio");
		newUser.setEmail("test1@gmail.com");
		newUser.setPassword("pwd123");
		newUser.setUsername("test1-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User createdUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "pwd123");

		AuthResponse authResp = authService.login(authRequest);

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setCategories(new HashSet<>(Set.of(new CategoryDTO("#wow"))));
		blogPostDTO.setContent("test-content");
		blogPostDTO.setTitle("test title");

		blogPostService.createOrUpdateBlogPost(blogPostDTO);

		ResponseEntity<BlogPostDetailsResponse> resp = template.exchange(
				createURLWithPort() + "/" + createdUser.getId() + "/post", HttpMethod.GET, null,
				BlogPostDetailsResponse.class);

		BlogPostDetailsResponse detailsResp = resp.getBody();

		assertNotNull(detailsResp);
		assertNotNull(detailsResp.getBlogPosts());
		assertEquals(detailsResp.getBlogPosts().get(0).getContent(), "test-content");

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testGetById() {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test1-bio");
		newUser.setEmail("test1@gmail.com");
		newUser.setPassword("pwd123");
		newUser.setUsername("test1-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User createdUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "pwd123");

		AuthResponse authResp = authService.login(authRequest);

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setCategories(new HashSet<>(Set.of(new CategoryDTO("#wow"))));
		blogPostDTO.setContent("test-content");
		blogPostDTO.setTitle("test title");

		blogPostService.createOrUpdateBlogPost(blogPostDTO);

		ResponseEntity<UserResponse> resp = template.exchange(createURLWithPort() + "/" + createdUser.getId(),
				HttpMethod.GET, null, UserResponse.class);

		UserResponse userResponse = resp.getBody();

		assertNotNull(userResponse);
		assertNotNull(userResponse.getBlogPosts());
		assertEquals("test-content", userResponse.getBlogPosts().get(0).getContent());

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testGetByIdWithResourceNotFoundFailure() {

		ResponseEntity<ErrorDetails> resp = template.exchange(createURLWithPort() + "/2", HttpMethod.GET, null,
				ErrorDetails.class);

		ErrorDetails errorDetails = resp.getBody();
		assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		assertNotNull(errorDetails);
		assertNotNull(errorDetails.getMsg());
		assertEquals("No user present with the provided id", errorDetails.getMsg());

	}

	@Test
	public void testGetAll() {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test1-bio");
		newUser.setEmail("test1@gmail.com");
		newUser.setPassword("pwd123");
		newUser.setUsername("test1-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User createdUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "pwd123");

		AuthResponse authResp = authService.login(authRequest);

		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setCategories(new HashSet<>(Set.of(new CategoryDTO("#wow"))));
		blogPostDTO.setContent("test-content");
		blogPostDTO.setTitle("test title");

		blogPostService.createOrUpdateBlogPost(blogPostDTO);

		ResponseEntity<List<UserResponse>> resp = template.exchange(createURLWithPort(), HttpMethod.GET, null,
				new ParameterizedTypeReference<List<UserResponse>>() {
				});

		List<UserResponse> userResponse = resp.getBody();

		assertNotNull(userResponse);
		assertNotNull(userResponse.get(0).getBlogPosts());
		assertEquals(userResponse.get(0).getBlogPosts().get(0).getContent(), "test-content");
	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testUpdate() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User savedUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());

		// want to update bio
		newUser.setBio("updated-test-bio");
		newUser.setId(savedUser.getId());
		HttpEntity<String> entity =

				new HttpEntity<>(objectMapper.writeValueAsString(newUser), headers);

		Map<String, Object> props = KafkaTestUtils.consumerProps("update-test-group", "true", embeddedKafkaBroker);

		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new StringDeserializer()).createConsumer();

		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, AppConstants.ADMINTOOL_TOPIC_NAME);

		ResponseEntity<UserResponse> resp = template.exchange(createURLWithPort(), HttpMethod.PUT, entity,
				UserResponse.class);

		UserResponse createdUser = resp.getBody();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);

		System.out.print(resp.getStatusCode());
		User dbUser = userDao.findById(savedUser.getId()).get();

		boolean isPresent = StreamSupport.stream(records.spliterator(), false)
				.anyMatch(record -> record.value().contains("Updated User " + dbUser.getId()));

		assertTrue(isPresent);

		assertNotNull(createdUser);
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertEquals(newUser.getUsername(), dbUser.getUsername());
		assertEquals("updated-test-bio", createdUser.getBio());
		assertEquals(newUser.getEmail(), dbUser.getEmail());

	}

	
	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testUpdateFailureWithCustomException() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User savedUser = userService.createUser(newUser);

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());

		// want to update bio
		newUser.setEmail("testupdated@gmail.com");
		newUser.setId(savedUser.getId());

		HttpEntity<String> entity =
		new HttpEntity<>(objectMapper.writeValueAsString(newUser), headers);
		ResponseEntity<ErrorDetails> resp = template.exchange(createURLWithPort(), HttpMethod.PUT, entity,
				ErrorDetails.class);

		ErrorDetails errorDetails = resp.getBody();


		assertNotNull(errorDetails);
		assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
		assertEquals("You can not change the registed email id. ", errorDetails.getMsg());

	}

	
	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testDelete() throws JsonProcessingException {

		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));

		User savedUser = userService.createUser(newUser);

		User dbUser = userDao.findById(savedUser.getId()).get();

		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());

		HttpEntity<String> entity =

				new HttpEntity<>(null, headers);

		Map<String, Object> props = KafkaTestUtils.consumerProps("delete-test-group", "true", embeddedKafkaBroker);

		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new StringDeserializer()).createConsumer();

		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, AppConstants.ADMINTOOL_TOPIC_NAME);

		ResponseEntity<UserResponse> resp = template.exchange(createURLWithPort() + "/" + savedUser.getId(),
				HttpMethod.DELETE, entity, UserResponse.class);

		UserResponse createdUser = resp.getBody();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);

		System.out.print(resp.getStatusCode());

		boolean isPresent = StreamSupport.stream(records.spliterator(), false)
				.anyMatch(record -> record.value().contains("Deleted User " + dbUser.getId()));

		assertEquals(HttpStatus.OK, resp.getStatusCode());
		assertTrue(isPresent);

		assertNotNull(createdUser);

		assertEquals(newUser.getUsername(), dbUser.getUsername());
		assertEquals(newUser.getEmail(), dbUser.getEmail());

	}
	
	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testDeleteFailureWithDoNotHavePermissionError() throws JsonProcessingException {
		
		//AUTHENTICATED USER ID - EDITOR
		UserDTO newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test234@gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID().toString());
		newUser.setRoles(new HashSet<>(List.of("ROLE_EDITOR")));

		User savedUser = userService.createUser(newUser);


		AuthRequest authRequest = new AuthRequest(newUser.getUsername(), "password123");

		AuthResponse authResp = authService.login(authRequest);

		headers.setBearerAuth(authResp.getToken());
		
		// WANT TO DELETE THE USER 	- ADMIN
		UserDTO newUser1 = new UserDTO();
		newUser1.setBio("test-bio");
		newUser1.setEmail("test456@gmail.com");
		newUser1.setPassword("password123");
		newUser1.setUsername("test-username" + UUID.randomUUID().toString());
		newUser1.setRoles(new HashSet<>(List.of("ROLE_ADMIN")));
		
		User savedUser1 = userService.createUser(newUser1);
		

		HttpEntity<String> entity =

				new HttpEntity<>(null, headers);

		
		ResponseEntity<ErrorDetails> resp = template.exchange(createURLWithPort() + "/" + savedUser1.getId(),
				HttpMethod.DELETE, entity, ErrorDetails.class);

		ErrorDetails errorDetails = resp.getBody();



		assertNotNull(errorDetails);
		assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
		assertEquals("You can not delete the user!", errorDetails.getMsg());

	}


}
