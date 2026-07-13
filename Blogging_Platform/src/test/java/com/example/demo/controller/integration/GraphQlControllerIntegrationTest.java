package com.example.demo.controller.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.example.demo.gqlservice.GraphQlService;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.AuthResponse;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
public class GraphQlControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private HttpGraphQlTester graphQlTester;

	@Autowired
	private GraphQlService graphQlService;
	@Autowired
	private BlogPostService blogPostService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserDao userDao;

	@Autowired
	private BlogPostDao postDao;

	@Autowired
	private CommentDao commentDao;

	@Autowired
	private CategoryDao categoryDao;

	@Autowired
	private EventDao eventDao;
	
	@Autowired
	private AuthService authService;

	private static HttpHeaders headers;

	private final ObjectMapper objectMapper = new ObjectMapper();


	@BeforeAll
	public static void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testSearchPosts() throws JsonProcessingException {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		userService.createUser(userDto);
		authService.login(new AuthRequest("test", "pass123"));
		CategoryDTO categoryDTO = new CategoryDTO();
		categoryDTO.setName("test-category");

		BlogPostDTO post = new BlogPostDTO();
		post.setTitle("post-title");
		post.setContent("post-content");
		post.setCategories(Set.of(categoryDTO));

		blogPostService.createOrUpdateBlogPost(post);

		graphQlTester.document("""
				        query Search{
				            searchPosts(keyword:"post"){
				                title,
				                content

				            }
				        }
				""").execute().path("searchPosts").entityList(BlogPostResponse.class).hasSize(1).satisfies(posts -> {
			assertEquals("post-title", posts.get(0).getTitle());

			assertEquals("post-content", posts.get(0).getContent());
		});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testGetPost() throws JsonProcessingException {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		userService.createUser(userDto);
		authService.login(new AuthRequest("test", "pass123"));
		CategoryDTO categoryDTO = new CategoryDTO();
		categoryDTO.setName("test-category");

		BlogPostDTO post = new BlogPostDTO();
		post.setTitle("post-title");
		post.setContent("post-content");
		post.setCategories(Set.of(categoryDTO));

		BlogPostResponse response1 = blogPostService.createOrUpdateBlogPost(post);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test1");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		userService.createUser(userDto1);
		authService.login(new AuthRequest("test1", "pass123"));
		CategoryDTO categoryDTO1 = new CategoryDTO();
		categoryDTO1.setName("test-category");

		BlogPostDTO post1 = new BlogPostDTO();
		post1.setTitle("post-title");
		post1.setContent("post-content");
		post1.setCategories(Set.of(categoryDTO));

		BlogPostResponse response2 = blogPostService.createOrUpdateBlogPost(post1);

		graphQlTester.document("""

				query Pagination{
				   getPosts(page: 0,size:2){
				    content
				   }
				}

				""").execute().path("getPosts").entityList(BlogPostResponse.class).hasSize(2).satisfies(posts -> {

			assertEquals("post-content", posts.get(0).getContent());

		});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testGetPinnedPostsOfTheUser() throws Exception {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user = userService.createUser(userDto);
		AuthResponse authResp = authService.login(new AuthRequest("test", "pass123"));
		CategoryDTO categoryDTO = new CategoryDTO();
		categoryDTO.setName("test-category");

		BlogPostDTO post = new BlogPostDTO();
		post.setTitle("post-title");
		post.setContent("post-content");
		post.setCategories(Set.of(categoryDTO));

		BlogPostResponse response = blogPostService.createOrUpdateBlogPost(post);

		graphQlService.postPinnedUnpinned(user.getId(), response.getId());

		HttpGraphQlTester authenticatedTester = graphQlTester.mutate().headers(headers -> {
			headers.setBearerAuth(authResp.getToken());
		}).build();
		String query = """
				query GetPinnedPostsOfTheUser {
				          getPinnedPostsOfTheUser(uId:%d){
				         
				            content
				           
				          }
				        }
				""".formatted(user.getId());
		authenticatedTester.document(query).execute().path("getPinnedPostsOfTheUser").entityList(BlogPostResponse.class)
				.hasSize(1).satisfies(pinnedPost -> {

					assertEquals("post-content", pinnedPost.get(0).getContent());

				});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testGetFollowers() throws JsonProcessingException {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test1");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		// set up data
		user1.setListfollowing(Set.of(user2));
		user2.setListfollowers(Set.of(user1));
		userDao.save(user1);
		userDao.save(user2);

		AuthResponse authResp = authService.login(new AuthRequest(user2.getUsername(), "pass123"));

		HttpGraphQlTester authenticatedTester = graphQlTester.mutate().headers(headers -> {
			headers.setBearerAuth(authResp.getToken());
		}).build();

		String query = """
					query GetFollowers{
				    getFollowers(uId:%d){
				            username
				    }
				}

				""".formatted(user2.getId());

		authenticatedTester.document(query).execute().path("getFollowers").entityList(UserResponse.class).hasSize(1)
				.satisfies(userResponse -> {

					assertEquals("test", userResponse.get(0).getUsername());

				});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testGetFollowerings() throws JsonProcessingException {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test1");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		// set up data
		user1.setListfollowing(Set.of(user2));
		user2.setListfollowers(Set.of(user1));
		userDao.save(user1);
		userDao.save(user2);

		AuthResponse authResp = authService.login(new AuthRequest(user1.getUsername(), "pass123"));

		HttpGraphQlTester authenticatedTester = graphQlTester.mutate().headers(headers -> {
			headers.setBearerAuth(authResp.getToken());
		}).build();

		String query = """
					query GetFollowings{
				         getFollowings(uId:%d){
				                username
				                }
				            }

				""".formatted(user1.getId());

		authenticatedTester.document(query).execute().path("getFollowings").entityList(UserResponse.class).hasSize(1)
				.satisfies(userResponse -> {

					assertEquals("test1", userResponse.get(0).getUsername());

				});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testTrendingPosts() throws JsonProcessingException {
		// top 10 posts based on like in desc order

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test" + UUID.randomUUID());
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test1" + UUID.randomUUID());
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		BlogPost blogpost1 = new BlogPost();
		blogpost1.setContent("fake-content-1");
		blogpost1.setLikes(100L);
		blogpost1.setTitle("fake-title-1");
		Category c1 = categoryDao.save(new Category("fake-category-1"));
		blogpost1.setCategories(Set.of(c1));
		blogpost1.setAuthor(user1);

		BlogPost blogpost2 = new BlogPost();
		blogpost2.setContent("fake-content-2");
		blogpost2.setLikes(99L);
		blogpost2.setTitle("fake-title-2");
		Category c2 = categoryDao.save(new Category("fake-category-2"));
		blogpost2.setCategories(Set.of(c2));
		blogpost2.setAuthor(user2);

		postDao.save(blogpost1);
		postDao.save(blogpost2);

		graphQlTester.document("""
				        query TrendingPosts{
				            trendingPosts{
				                   content,
				                   createAt
				            }
				        }
				""").execute().path("trendingPosts").entityList(BlogPostResponse.class).hasSize(2)
				.satisfies(trendingPost -> assertEquals("fake-content-1", trendingPost.get(0).getContent()));

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testUserLikedPost() throws JsonProcessingException {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test1");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		BlogPost blogpost1 = new BlogPost();
		blogpost1.setContent("fake-content-1");
		blogpost1.setLikes(100L);
		blogpost1.setTitle("fake-title-1");
		Category c1 = categoryDao.save(new Category("fake-category-1"));
		blogpost1.setCategories(Set.of(c1));
		blogpost1.setAuthor(user1);

		BlogPost blogpost2 = new BlogPost();
		blogpost2.setContent("fake-content-2");
		blogpost2.setLikes(99L);
		blogpost2.setTitle("fake-title-2");
		Category c2 = categoryDao.save(new Category("fake-category-2"));
		blogpost2.setCategories(Set.of(c2));
		blogpost2.setAuthor(user2);

		postDao.save(blogpost1);
		postDao.save(blogpost2);

		// user is liking some posts

		UserDTO userDto3 = new UserDTO();
		userDto3.setUsername("test3");
		userDto3.setPassword("pass123");
		userDto3.setEmail("test3@gmail.com");
		userDto3.setRoles(Set.of("ROLE_EDITOR"));

		User user3 = userService.createUser(userDto3);
		user3.setLikedBlogPosts(List.of(blogpost1, blogpost2));
		userDao.save(user3);
		AuthResponse authResponse = authService.login(new AuthRequest(user3.getUsername(), "pass123"));

		HttpGraphQlTester authenticatedTester = graphQlTester.mutate()
				.headers(header -> header.setBearerAuth(authResponse.getToken())).build();

		String query = """
				        query   UserLikedPost{
				           userLikedPost(uId:%d){
				                   content,
				                   createAt
				            }
				        }
				""".formatted(user3.getId());

		authenticatedTester.document(query).execute().path("userLikedPost").entityList(BlogPostResponse.class)
				.hasSize(2).satisfies(post -> {
					assertEquals("fake-content-1", post.get(0).getContent());
				});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testBlockedUsers() throws JsonProcessingException {
		UserDTO userDto = new UserDTO();
		userDto.setUsername("test");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test1");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		BlogPost blogpost1 = new BlogPost();
		blogpost1.setContent("fake-content-1");
		blogpost1.setLikes(100L);
		blogpost1.setTitle("fake-title-1");
		Category c1 = categoryDao.save(new Category("fake-category-1"));
		blogpost1.setCategories(Set.of(c1));
		blogpost1.setAuthor(user1);

		BlogPost blogpost2 = new BlogPost();
		blogpost2.setContent("fake-content-2");
		blogpost2.setLikes(99L);
		blogpost2.setTitle("fake-title-2");
		Category c2 = categoryDao.save(new Category("fake-category-2"));
		blogpost2.setCategories(Set.of(c2));
		blogpost2.setAuthor(user2);

		postDao.save(blogpost1);
		postDao.save(blogpost2);

		// user is liking some posts

		UserDTO userDto3 = new UserDTO();
		userDto3.setUsername("test3");
		userDto3.setPassword("pass123");
		userDto3.setEmail("test3@gmail.com");
		userDto3.setRoles(Set.of("ROLE_EDITOR"));

		User user3 = userService.createUser(userDto3);
		user3.setBlockedUsers(Set.of(user1, user2));
		user1.setBlockedByUsers(Set.of(user3));
		user2.setBlockedByUsers(Set.of(user3));

		userDao.save(user3);
		userDao.save(user2);
		userDao.save(user1);
		String query = """
				query BlockedUsers{
				  blockedUsers (uId:%d){
				    id
				    username
				    }
				}
				""".formatted(user3.getId());

		AuthResponse authResponse = authService.login(new AuthRequest(user3.getUsername(), "pass123"));
		GraphQlTester authTester = graphQlTester.mutate().headers(header -> {
			header.setBearerAuth(authResponse.getToken());
		}).build();

		authTester.document(query).execute().path("blockedUsers").entityList(UserResponse.class).hasSize(2)
				.satisfies(userResponse -> {
					assertEquals("test", userResponse.get(0).getUsername());
				});

	}

	// mutation

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testSetReaction() throws JsonProcessingException {

		UserDTO userDto = new UserDTO();
		userDto.setUsername("test4");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test5");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		BlogPost blogpost1 = new BlogPost();
		blogpost1.setContent("fake-content-1");
		blogpost1.setTitle("fake-title-1");
		Category c1 = categoryDao.save(new Category("fake-category-1"));
		blogpost1.setCategories(Set.of(c1));
		blogpost1.setAuthor(user1);
		postDao.save(blogpost1);

		// user is liking some posts

		UserDTO userDto3 = new UserDTO();
		userDto3.setUsername("test6");
		userDto3.setPassword("pass123");
		userDto3.setEmail("test3@gmail.com");
		userDto3.setRoles(Set.of("ROLE_EDITOR"));

		User user3 = userService.createUser(userDto3);

		AuthResponse authResponse = authService.login(new AuthRequest(user3.getUsername(), "pass123"));

		GraphQlTester authTester = graphQlTester.mutate().headers(header -> {
			header.setBearerAuth(authResponse.getToken());
		}).build();

		String query = """

				mutation SetReaction {
				  setReaction(
				     request : {
				      bpId: %d,
				      uId: %d,
				      reaction: true
				    }
				  ) {
				  	id
				    content
				    createAt
				    likes
				  }
				}

				""".formatted(blogpost1.getId(), user3.getId());

		BlogPostResponse blogPost = authTester.document(query).execute().path("setReaction")
				.entity(BlogPostResponse.class).get();

	Event event =	eventDao.findByTransactionIdAndEventType(blogpost1.getId()+"", EventType.LIKE).get();
		assertEquals("1", blogPost.getLikes().toString());
		assertEquals("fake-content-1", blogPost.getContent());
		assertNotNull(event);
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testPinnedPost() throws JsonProcessingException {

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test51");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test11@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		BlogPost blogpost1 = new BlogPost();
		blogpost1.setContent("fake-content-1");
		blogpost1.setTitle("fake-title-1");
		Category c1 = categoryDao.save(new Category("fake-category-1"));
		blogpost1.setCategories(Set.of(c1));
		blogpost1.setAuthor(user2);
		BlogPost createdBlogpost = postDao.save(blogpost1);

		// user is liking some posts

		UserDTO userDto3 = new UserDTO();
		userDto3.setUsername("test6");
		userDto3.setPassword("pass123");
		userDto3.setEmail("test3@gmail.com");
		userDto3.setRoles(Set.of("ROLE_EDITOR"));

		User user3 = userService.createUser(userDto3);

		AuthResponse authResponse = authService.login(new AuthRequest(user3.getUsername(), "pass123"));

		GraphQlTester authTester = graphQlTester.mutate().headers(header -> {
			header.setBearerAuth(authResponse.getToken());
		}).build();

		String query = """

				mutation PinUnpinPost {
				 pinUnpinPost(uId:%d , bpId: %d ){
				
				      content
				      id
				      author{
				      username
				     }
				  
				}
				}

				""".formatted(user3.getId(), createdBlogpost.getId());

		BlogPostResponse pinnedBlogPost = authTester.document(query).execute().path("pinUnpinPost")
				.entity(BlogPostResponse.class).get();

		assertEquals("fake-content-1", pinnedBlogPost.getContent());
		assertEquals("test51", pinnedBlogPost.getAuthor().getUsername());
		Event event =	eventDao.findByTransactionIdAndEventType(blogpost1.getId()+"", EventType.PIN).get();
		assertEquals("fake-content-1", pinnedBlogPost.getContent());
		assertNotNull(event);
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testFollowOrUnFollowAuthor() throws JsonProcessingException {
		UserDTO userDto = new UserDTO();
		userDto.setUsername("test4");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test5");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		// user2 will follow user1
		AuthResponse authResponse = authService.login(new AuthRequest(user2.getUsername(), "pass123"));

		GraphQlTester authTester = graphQlTester.mutate().headers(header -> {
			header.setBearerAuth(authResponse.getToken());
		}).build();

		String query = """

				mutation FollowOrUnFollowAuthor{
				    followOrUnFollowAuthor(follower: %d, followee:%d){
				        username
				        id
				    }
				}

				""".formatted(user2.getId(), user1.getId());

		List<UserResponse> userResp = authTester.document(query).execute().path("followOrUnFollowAuthor")
				.entityList(UserResponse.class).get();
			
		System.out.println("Sree"+userResp.get(0));
		// followeeUser
		assertEquals("test4", userResp.get(0).getUsername());
		// followerUser
		assertEquals("test5", userResp.get(1).getUsername());
		
		Event event = eventDao.findByTransactionIdAndEventType(user2.getId()+"", EventType.FOLLOW).get();
		
		assertNotNull(event);
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(TransactionType.USER, event.getTransactionType());
		
		

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testFollowOrUnFollowAuthor_SameFollowFollowee_FailureWithFollowUnFollowException() throws JsonProcessingException {
		UserDTO userDto = new UserDTO();
		userDto.setUsername("test4");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		String query = """

				mutation FollowOrUnFollowAuthor{
				    followOrUnFollowAuthor(follower: %d, followee:%d){
				        username
				    }
				}

				""".formatted(user1.getId(), user1.getId());
		AuthResponse authResponse = authService.login(new AuthRequest(user1.getUsername(), "pass123"));

		GraphQlTester authTester = graphQlTester.mutate().headers(header -> {
			header.setBearerAuth(authResponse.getToken());
		}).build();

		authTester.document(query).execute().errors().satisfy(errors -> {
			assertEquals(1, errors.size());
			assertEquals("You can not follow yourself!", errors.get(0).getMessage());
			assertEquals(ErrorType.BAD_REQUEST, errors.get(0).getErrorType());
		});

	}

	@Test
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testBlockUser() throws JsonProcessingException {
		UserDTO userDto = new UserDTO();
		userDto.setUsername("test4");
		userDto.setPassword("pass123");
		userDto.setEmail("test@gmail.com");
		userDto.setRoles(Set.of("ROLE_EDITOR"));
		User user1 = userService.createUser(userDto);

		UserDTO userDto1 = new UserDTO();
		userDto1.setUsername("test5");
		userDto1.setPassword("pass123");
		userDto1.setEmail("test1@gmail.com");
		userDto1.setRoles(Set.of("ROLE_EDITOR"));
		User user2 = userService.createUser(userDto1);

		// user1 wants to block user2

		AuthResponse authResponse = authService.login(new AuthRequest(user1.getUsername(), "pass123"));

		GraphQlTester authTester = graphQlTester.mutate().headers(header -> {
			header.setBearerAuth(authResponse.getToken());
		}).build();

		String query = """
				 mutation BlockUser{
				           blockUser (blockerId: %d, blockedUserId:%d){
				                username
				                id
				            }
				        }
				""".formatted(user1.getId(), user2.getId());

	List<UserResponse>  userReps=	authTester.document(query).execute().path("blockUser").entityList(UserResponse.class).get();
		
	assertTrue(userReps.size() == 2);
	assertEquals("test4", user1.getUsername());
	assertEquals("test5", user2.getUsername());
	
	Event event = eventDao.findByTransactionIdAndEventType(user1.getId()+"", EventType.BLOCK).get();
	
	assertNotNull(event);
	assertEquals(EventStatus.PENDING, event.getStatus());
	assertEquals(TransactionType.USER, event.getTransactionType());
	
	}

}
