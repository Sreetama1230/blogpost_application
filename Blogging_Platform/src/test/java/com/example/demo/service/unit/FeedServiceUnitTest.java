package com.example.demo.service.unit;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.FeedItem;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.service.FeedService;

@ExtendWith(SpringExtension.class)
public class FeedServiceUnitTest {

	@Mock
	private BlogPostDao blogPostDao;

	@Mock
	private UserDao userDao;

	@InjectMocks
	private FeedService feedService;

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

		HashSet<BlogPost> set = new HashSet<>();
		set.add(blogPost);
		category.setBlogPosts(set);
		user.setBlogPosts(new ArrayList<>(List.of(blogPost)));

	}

	@Test
	public void testTimeline_loggedOutUser() {

		try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
			utils.when(SecurityUtils::getCurrentUserId).thenThrow(new IllegalStateException("User is not authneticated"));
			PageRequest pageable;
			pageable = PageRequest.of(0, 10);
			List<BlogPost> page = new PageImpl<>(List.of(blogPost), pageable, List.of(blogPost).size()).toList();

			when(blogPostDao.findPostByCreateTimeAndReactCount(pageable)).thenReturn(page);

			List<FeedItem> feedItems = feedService.timeline(0, 10);
			assertEquals(1, feedItems.size());
			assertEquals("fake-title", feedItems.get(0).getBlogDescription());
		}
	}

	@Test
	public void testTimeline_loggedInUser() {
		try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
			blogPost.setLikes(100L);
			blogPost.setDislikes(0L);

			BlogPost p2 = new BlogPost("title-test", "content-test", user, Set.of(new Category("category-test")),
					new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now());
			p2.setLikes(50L);
			p2.setDislikes(0L);
			user1.setListfollowers(Set.of(user));
			user1.setFollowers(1L);

			user1.setListfollowing(Set.of(user));
			user1.setFollowing(1L);

			utils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

			when(userDao.findById(2L)).thenReturn(Optional.of(user1));

			when(blogPostDao.findPostsInListAuthors(any(), any())).thenReturn(List.of(p2));

			when(blogPostDao.findPostsNotInListOfAuthor(any(), any())).thenReturn(List.of(blogPost));

			List<FeedItem> feedItems = feedService.timeline(0, 10);
			assertEquals(2, feedItems.size());
			assertEquals(blogPost.getTitle(), feedItems.get(0).getBlogDescription());
			assertEquals(p2.getTitle(), feedItems.get(1).getBlogDescription());

		}
	}

}
