package com.example.demo.controller.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import com.example.demo.dao.*;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.model.*;
import org.apache.kafka.clients.consumer.Consumer;
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

import com.example.demo.constants.AppConstants;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.CommentDTO;
import com.example.demo.dto.CommentReact;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.Reaction;
import com.example.demo.error.ErrorDetails;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CategoryResponse;
import com.example.demo.response.CommentResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CommentControllerIntegrationTest {

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
	private CommentDao commentDao;

	@Autowired
	private AuthService authService;

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
		return "http://localhost:" + port + "/comment";

	}

	BlogPostDTO blogPostDTO;
	UserDTO newUser;
	CategoryDTO categoryDTO;
	CommentDTO commentDTO;
	UserDTO authUser;
	AuthRequest authRequest;
	AuthResponse authResp;
	AuthRequest authRequest1;
	AuthResponse authResp1;
	BlogPostResponse blogPostResponse;
	User user1;
	User user2;

	@BeforeEach
	public void setUp() throws JsonProcessingException {

		newUser = new UserDTO();
		newUser.setBio("test-bio");
		newUser.setEmail("test@12345gmail.com");
		newUser.setPassword("password123");
		newUser.setUsername("test-username" + UUID.randomUUID());
		newUser.setRoles(Set.of("ROLE_ADMIN"));

		user1 = userService.createUser(newUser);

		authRequest1 = new AuthRequest(newUser.getUsername(), "password123");
		authResp1 = authService.login(authRequest1);

		categoryDTO = new CategoryDTO();
		categoryDTO.setName("fake-category");

		blogPostDTO = new BlogPostDTO();
		blogPostDTO.setContent("Fake Content");
		blogPostDTO.setTitle("Fake Title");
		blogPostDTO.setCategories(new HashSet<>(Set.of(categoryDTO)));

		blogPostResponse = blogPostService.createOrUpdateBlogPost(blogPostDTO);

		commentDTO = new CommentDTO();
		commentDTO.setMessage("test-comment");

		// current authenticated user details
		authUser = new UserDTO();
		authUser.setBio("test-bio");
		authUser.setEmail("testauth@gmail.com");
		authUser.setPassword("password123");
		authUser.setUsername("test-username" + UUID.randomUUID());
		authUser.setRoles(Set.of("ROLE_USER"));
		authRequest = new AuthRequest(authUser.getUsername(), "password123");
		user2 = userService.createUser(authUser);
		authResp = authService.login(authRequest);

	}

    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testAddComment() throws JsonProcessingException {

		headers.setBearerAuth(authResp.getToken());

		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(commentDTO), headers);

		ResponseEntity<BlogPostResponse> response = template.exchange(
				createURLWithPort() + "?blogPostId=" + blogPostResponse.getId(), HttpMethod.POST, entity,
				BlogPostResponse.class);

	
		BlogPostResponse postResponse = response.getBody();

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(1L, postResponse.getComments().size());

        Event event = eventDao.findByTransactionIdAndEventType( postResponse.getComments().get(0).getId()+"" , EventType.CREATE).get();

        assertNotNull(event);
        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(TransactionType.COMMENT, event.getTransactionType());

	}


    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testUpdateComment() throws JsonProcessingException {

		BlogPost post = postDao.findById(blogPostResponse.getId()).get();

		Comment commet = new Comment(commentDTO.getMessage(), user2, post, LocalDateTime.now(), 0L, 0L, 0L,
				new HashSet<>());

		commentDao.save(commet);
		post.setComments(new ArrayList<>(List.of(commet)));
		postDao.save(post);

		commentDTO.setCommentId(commet.getId());
		commentDTO.setMessage("updated-comment");

		// authenticated with authUser
		headers.setBearerAuth(authResp.getToken());
		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(commentDTO), headers);

		ResponseEntity<BlogPostResponse> response = template.exchange(
				createURLWithPort() + "?blogPostId=" + blogPostResponse.getId(), HttpMethod.PUT, entity,
				BlogPostResponse.class);

		BlogPostResponse postResponse = response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1L, postResponse.getComments().size());
        assertNotNull(postResponse.getComments().get(0));
		assertEquals("updated-comment(edited)", response.getBody().getComments().get(0).getContent());

        Event event = eventDao.findByTransactionIdAndEventType( postResponse.getComments().get(0).getId()+"" , EventType.UPDATE).get();

        assertNotNull(event);
        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(TransactionType.COMMENT, event.getTransactionType());
	}


    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testGetCommentById() throws JsonProcessingException {

		BlogPost post = postDao.findById(blogPostResponse.getId()).get();

		Comment commet = new Comment(commentDTO.getMessage(), user2, post, LocalDateTime.now(), 0L, 0L, 0L,
				new HashSet<>());
		commentDao.save(commet);
		post.setComments(new ArrayList<>(List.of(commet)));
		postDao.save(post);
		headers.setBearerAuth(authResp.getToken());

		ResponseEntity<CommentResponse> response = template.exchange(createURLWithPort() + "/" + commet.getId(),
				HttpMethod.GET, null, CommentResponse.class);

		CommentResponse commentResponse = response.getBody();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("test-comment", commentResponse.getContent());

	}

    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testGetDeleteById() throws JsonProcessingException {

		BlogPost post = postDao.findById(blogPostResponse.getId()).get();

		Comment commet = new Comment(commentDTO.getMessage(), user2, post, LocalDateTime.now(), 0L, 0L, 0L,
				new HashSet<>());
		commentDao.save(commet);
		post.setComments(new ArrayList<>(List.of(commet)));
		postDao.save(post);
		headers.setBearerAuth(authResp.getToken());
		HttpEntity<String> entity = new HttpEntity<>(null, headers);
		ResponseEntity<CommentResponse> response = template.exchange(
				createURLWithPort() + "/" + commet.getId() + "/blogpost/" + post.getId(), HttpMethod.DELETE, entity,
				CommentResponse.class);

		CommentResponse commentResponse = response.getBody();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("test-comment", commentResponse.getContent());

	}

    @Test
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testReactComment() throws JsonProcessingException {
		BlogPost post = postDao.findById(blogPostResponse.getId()).get();

		headers.setBearerAuth(authResp.getToken());

		Comment commet = new Comment(commentDTO.getMessage(), user2, post, LocalDateTime.now(), 0L, 0L, 0L,
				new HashSet<>());
		commentDao.save(commet);
		post.setComments(new ArrayList<>(List.of(commet)));
		postDao.save(post);

		CommentReact react = new CommentReact();
		react.setId(commet.getId());
		react.setReaction(Reaction.LOVE);
		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(react), headers);

		ResponseEntity<CommentResponse> response = template.exchange(createURLWithPort() + "/react", HttpMethod.POST,
				entity, CommentResponse.class);

		CommentResponse commentResponse = response.getBody();

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("1", commentResponse.getLoveCount().toString());
		assertEquals("0", commentResponse.getFunnyCount().toString());
		assertEquals("0", commentResponse.getLikeCount().toString());

	}

}
