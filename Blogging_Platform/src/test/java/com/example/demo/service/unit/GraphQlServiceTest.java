package com.example.demo.service.unit;

import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.ReactDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.FollowUnFollowException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.gqlservice.GraphQlService;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class GraphQlServiceTest {

	@Mock
	private BlogPostDao blogPostDao;

	@Mock
	private UserDao userDao;

	@Mock
	private UserService userService;

	@Mock
	private BlogPostService blogPostService;

	@InjectMocks
	GraphQlService postsService;

	@Mock
	private EventDao eventDao;
	@Mock
	ObjectMapper objectMapper;

	private User user;
	private User user1;
	private BlogPost blogPost;
	private Category category;
	PageRequest pageable;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setUsername("fake-username");
		user.setEmail("fake-email");

		user1 = new User();
		user1.setId(2L);
		user1.setUsername("fake-username-2");
		user1.setEmail("fake-email-2");

		category = new Category();
		category.setName("fake-category");
		category.setId(1L);

		blogPost = new BlogPost();
		blogPost.setId(1L);
		blogPost.setTitle("fake-title");
		blogPost.setContent("fake-content");
		blogPost.setComments(new ArrayList<>());
		blogPost.setAuthor(user);
		blogPost.setCategories(Set.of(category));
		blogPost.setLikes(0L);

		pageable = PageRequest.of(0, 10);
		HashSet<BlogPost> set = new HashSet<>();
		set.add(blogPost);
		category.setBlogPosts(set);
		user.setBlogPosts(new ArrayList<>(List.of(blogPost)));

	}

	@Test
	void testSearchPosts_Success() {

		String keyword = "fake";
		when(blogPostDao.searchPosts(keyword)).thenReturn(List.of(blogPost));

		List<BlogPostResponse> blogPostResponses = postsService.searchPosts(keyword);
		assertEquals(List.of(blogPost).size(), blogPostResponses.size());
		assertEquals(blogPost.getContent(), blogPostResponses.get(0).getContent());

	}

	@Test
	void testGetPosts() {

		List<BlogPost> list = List.of(blogPost);
		Page<BlogPost> page = new PageImpl<>(list);

		when(blogPostDao.findAll(pageable)).thenReturn(page);
		Page<BlogPostResponse> blogPostResponses = postsService.getPosts(0, 10);

		assertEquals(page.getTotalPages(), blogPostResponses.getTotalPages());
		assertEquals(page.getTotalElements(), blogPostResponses.getTotalElements());

	}

	@Test
	void testGetPinnedPostsOfTheUser() {
		blogPost.setPinnedBy(Set.of(user));
		user.setPinnedBlogPosts(Set.of(blogPost));

		// logged in user
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		List<BlogPostResponse> blogPosts = postsService.getPinnedPostsOfTheUser(1L);

		assertEquals("fake-content", blogPosts.get(0).getContent());
		assertEquals("fake-title", blogPosts.get(0).getTitle());

	}

	@Test
	void testGetPinnedPostsOfTheUser_WithInvalidUserId_FailureWithResourceNotFoundException() {
		blogPost.setPinnedBy(Set.of(user));
		user.setPinnedBlogPosts(Set.of(blogPost));

		// logged in user
		when(userDao.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
			postsService.getPinnedPostsOfTheUser(1L);
		});

		assertNotNull(ex);
		assertEquals("user is not valid", ex.getMessage());
	}

	@Test
	void testGetFollowers() {
		Set<User> followersUser = new HashSet<>();
		followersUser.add(user1);
		user.setListfollowers(followersUser);
		user.setFollowers(1L);
		// user1 is follower of user

		Set<User> followingUser = new HashSet<>();
		followingUser.add(user);
		user1.setListfollowing(followingUser);
		user1.setFollowing(1L);

		when(userDao.findById(1L)).thenReturn(Optional.of(user));

		List<UserResponse> followers = postsService.getFollowers(1L);
		assertEquals(1, followers.size());
		assertEquals("fake-username-2", followers.get(0).getUsername());

	}

	@Test
	void testGetFollowerings() {

		Set<User> followersUser = new HashSet<>();
		followersUser.add(user1);
		user.setListfollowers(followersUser);
		user.setFollowers(1L);
		// user1 is follower of user

		Set<User> followingUser = new HashSet<>();
		followingUser.add(user);
		user1.setListfollowing(followingUser);
		user1.setFollowing(1L);

		when(userDao.findById(2L)).thenReturn(Optional.of(user1));

		List<UserResponse> followerings = postsService.getFollowings(2L);
		assertEquals(1, followerings.size());
		assertEquals("fake-username", followerings.get(0).getUsername());

	}

	@Test
	void testTrendingPosts() {
		Page<BlogPost> page = new PageImpl<>(List.of(blogPost), pageable, List.of(blogPost).size());
		when(blogPostDao.findTopNByOrderByLikesDesc(pageable)).thenReturn(page);

		Page<BlogPostResponse> blogPostResponses = postsService.trendingPosts();

		assertEquals(page.getTotalPages(), blogPostResponses.getTotalPages());
		assertEquals(page.getTotalElements(), blogPostResponses.getTotalElements());
	}

	@Test
	void testLikedBlogPostResponses() {
		user.setLikedBlogPosts(List.of(blogPost));
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		List<BlogPostResponse> blogPostResponses = postsService.userLikedPost(1L);

		assertEquals(user.getLikedBlogPosts().size(), blogPostResponses.size());
		assertEquals(user.getLikedBlogPosts().get(0).getContent(), blogPostResponses.get(0).getContent());

	}

	@Test
	void testGetBlockedUsers() {
		Set<User> users = new HashSet<>();
		users.add(user1);
		user.setBlockedUsers(users);
		when(userDao.findById(1L)).thenReturn(Optional.of(user));
		List<UserResponse> userResponses = postsService.getBlockedUsers(1L);

		assertEquals(1, userResponses.size());
		assertEquals(user1.getUsername(), userResponses.get(0).getUsername());
	}

	// @mutation
	@Test
	void testSetLikeReaction() throws Exception {
		ReactDTO request = new ReactDTO(1L, true, 1L);
		when(blogPostService.getById(request.getBpId())).thenReturn(blogPost);
		when(userService.getbyId(request.getuId())).thenReturn(user);
		long n = blogPost.getLikes();

		BlogPostResponse blogPostResponse = postsService.setReaction(request);

		String payload = objectMapper.writeValueAsString(request);

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		verify(eventDao).save(captor.capture());

		Event event = captor.getValue();

		assertEquals(EventType.LIKE, event.getEventType());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(blogPostResponse.getId() + "", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

		assertNotNull(blogPostResponse);
		assertEquals(Optional.of(n + 1L).get(), blogPostResponse.getLikes());
		assertEquals(blogPost.getContent(), blogPostResponse.getContent());

	}

	@Test
	void testSetDislikeReaction() throws Exception {
		ReactDTO request = new ReactDTO(1L, false, 1L);
		when(blogPostService.getById(request.getBpId())).thenReturn(blogPost);
		when(userService.getbyId(request.getuId())).thenReturn(user);
		long n = blogPost.getLikes();

		BlogPostResponse blogPostResponse = postsService.setReaction(request);

		String payload = objectMapper.writeValueAsString(request);

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		verify(eventDao).save(captor.capture());

		Event event = captor.getValue();

		assertEquals(EventType.DISLIKE, event.getEventType());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(blogPostResponse.getId() + "", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

		assertNotNull(blogPostResponse);
		assertEquals(Optional.of(n).get(), blogPostResponse.getLikes());
		assertEquals(Optional.of(n + 1L).get(), blogPostResponse.getDislikes());
		assertEquals(blogPost.getContent(), blogPostResponse.getContent());

	}

	@Test
	void testSetReaction_WithInvalidId_FailureWithResourceNotFoundException() {
		ReactDTO request = new ReactDTO(1L, true, 1L);

		when(blogPostService.getById(request.getBpId())).thenThrow(ResourceNotFoundException.class);

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
			postsService.setReaction(request);
		});

		assertNotNull(ex);

	}

	@Test
	void testPinnedPost() throws Exception {
		when(blogPostService.getById(1L)).thenReturn(blogPost);

		when(userService.getbyId(1L)).thenReturn(user);
		int n = user.getPinnedBlogPosts().size();

		BlogPostResponse pinnedBlogPost = postsService.postPinnedUnpinned(1L, 1L);
		assertNotNull(pinnedBlogPost);
		assertEquals(n + 1, user.getPinnedBlogPosts().size());
		assertEquals((Long) 1L, pinnedBlogPost.getId());
		assertEquals(blogPost.getContent(), pinnedBlogPost.getContent());

		String payload = objectMapper.writeValueAsString("User Id: 1 BlogPost Id: 1");
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventDao).save(captor.capture());
		Event event = captor.getValue();

		assertEquals(EventType.PIN, event.getEventType());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(pinnedBlogPost.getId() + "", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

	}

	@Test
	void testUnPinnedPost() throws Exception {

		blogPost.setPinnedBy(new HashSet<>(Set.of(user)));
		user.setPinnedBlogPosts(new HashSet<>(Set.of(blogPost)));

		when(blogPostService.getById(1L)).thenReturn(blogPost);
		when(userService.getbyId(1L)).thenReturn(user);

		int n = user.getPinnedBlogPosts().size();
		BlogPostResponse pinnedBlogPost = postsService.postPinnedUnpinned(1L, 1L);

		assertNotNull(pinnedBlogPost);
		assertEquals((Long) 1L, pinnedBlogPost.getId());
		assertEquals(blogPost.getContent(), pinnedBlogPost.getContent());

		String payload = objectMapper.writeValueAsString("User Id: 1 BlogPost Id: 1");
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventDao).save(captor.capture());
		Event event = captor.getValue();

		assertEquals(EventType.UNPIN, event.getEventType());
		assertEquals(TransactionType.BLOGPOST, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(pinnedBlogPost.getId() + "", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

	}

	@Test
	void testPinnedPost_WithInvalidId_FailureWithResourceNotFoundException() {
		when(blogPostService.getById(1L)).thenThrow(ResourceNotFoundException.class);

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
			postsService.postPinnedUnpinned(1L, 1L);

		});

		assertNotNull(ex);

	}

	@Test
	void testFollowAuthor() throws Exception {

		when(userService.getbyId(1L)).thenReturn(user);
		when(userService.getbyId(2L)).thenReturn(user1);

		when(userDao.save(user)).thenReturn(user);
		when(userDao.save(user1)).thenReturn(user1);

		List<UserResponse> userResponses = postsService.followOrUnFollowAuthor(2L, 1L);

		String payload = objectMapper.writeValueAsString("Follower Id: 2 Followee Id: 1");
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventDao).save(captor.capture());
		Event event = captor.getValue();

		assertEquals(EventType.FOLLOW, event.getEventType());
		assertEquals(TransactionType.USER, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals("2", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

		assertEquals(true, user.getListfollowers().contains(user1));
		assertEquals(true, user1.getListfollowing().contains(user));
		assertEquals(2, userResponses.size());
		assertEquals(user.getUsername(), userResponses.get(0).getUsername());
		assertEquals(user1.getUsername(), userResponses.get(1).getUsername());

	}

	@Test
	void testUnFollowAuthor() throws Exception {

		user.getListfollowing().add(user1);
		user1.getListfollowers().add(user);
		user.setFollowing(user.getFollowing());
		user1.setFollowers(user1.getFollowers());

		when(userService.getbyId(1L)).thenReturn(user1);
		when(userService.getbyId(2L)).thenReturn(user);

		when(userDao.save(user)).thenReturn(user);
		when(userDao.save(user1)).thenReturn(user1);

		List<UserResponse> userResponses = postsService.followOrUnFollowAuthor(2L, 1L);

		String payload = objectMapper.writeValueAsString("Follower Id: 2 Followee Id: 1");
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventDao).save(captor.capture());
		Event event = captor.getValue();

		assertEquals(EventType.UNFOLLOW, event.getEventType());
		assertEquals(TransactionType.USER, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals("2", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

		assertEquals(false, user.getListfollowers().contains(user1));
		assertEquals(false, user1.getListfollowing().contains(user));
		assertEquals(2, userResponses.size());
		assertEquals(user.getUsername(), userResponses.get(1).getUsername());
		assertEquals(user1.getUsername(), userResponses.get(0).getUsername());

	}

	@Test
	void testFollowOrUnFollowAuthor_SameFollowerFollowee_FailureWithFollowUnFollowException() {

		FollowUnFollowException followUnFollowException = assertThrows(FollowUnFollowException.class, () -> {
			postsService.followOrUnFollowAuthor(1L, 1L);
		});

		assertNotNull(followUnFollowException);
		assertEquals("You can not follow yourself!", followUnFollowException.getMessage());

	}

	@Test
	void testFollowOrUnFollowAuthor_WithInvalidId_ResourceNotFoundException() {

		when(userService.getbyId(1L)).thenThrow(ResourceNotFoundException.class);
		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			postsService.followOrUnFollowAuthor(1L, 2L);
		});

		assertNotNull(resourceNotFoundException);

	}

	@Test
	void testBlockUser_WithInvalidId_ResourceNotFoundException() {

		when(userService.getbyId(1L)).thenThrow(ResourceNotFoundException.class);

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			postsService.blockUnblockUser(1L, 2L);
		});

		assertNotNull(resourceNotFoundException);
		assertEquals("Either the blocker or the blocking user is not present!", resourceNotFoundException.getMessage());

	}

	@Test
	void testBlockUser() throws Exception {
		when(userService.getbyId(1L)).thenReturn(user);
		when(userService.getbyId(2L)).thenReturn(user1);

		when(userDao.save(user)).thenReturn(user);
		when(userDao.save(user1)).thenReturn(user1);
		

		List<UserResponse> userResponses = postsService.blockUnblockUser(1L, 2L);
		
		String payload = objectMapper.writeValueAsString("Blocker Id: 1 BlockedUser Id: 2");
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventDao).save(captor.capture());
		Event event = captor.getValue();

		assertEquals(EventType.BLOCK, event.getEventType());
		assertEquals(TransactionType.USER, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals("1", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());
		
		
		assertEquals(true, user.getBlockedUsers().contains(user1));
		assertEquals(2, userResponses.size());
		assertEquals(user.getUsername(), userResponses.get(0).getUsername());
		assertEquals(user1.getUsername(), userResponses.get(1).getUsername());

	}

	
	@Test
	void testUnblockUser() throws Exception {

		user.getBlockedUsers().add(user1);
		user1.getBlockedByUsers().add(user);
		
	
		when(userService.getbyId(1L)).thenReturn(user);
		when(userService.getbyId(2L)).thenReturn(user1);

		when(userDao.save(user)).thenReturn(user);
		when(userDao.save(user1)).thenReturn(user1);

		List<UserResponse> userResponses = postsService.blockUnblockUser(1L, 2L);
		String payload = objectMapper.writeValueAsString("Blocker Id: 1 BlockedUser Id: 2");
		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventDao).save(captor.capture());
		Event event = captor.getValue();

		assertEquals(EventType.UNBLOCK, event.getEventType());
		assertEquals(TransactionType.USER, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals("1", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());
		
		
		assertEquals(false, user.getBlockedUsers().contains(user1));
		assertEquals(2, userResponses.size());
		assertEquals(user.getUsername(), userResponses.get(0).getUsername());
		assertEquals(user1.getUsername(), userResponses.get(1).getUsername());

	}
}
