package com.example.demo.controller.unit;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.demo.config.JwtAuthFilter;
import com.example.demo.config.JwtUtils;
import com.example.demo.controller.FeedController;
import com.example.demo.dto.FeedItem;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.service.FeedService;


@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FeedController.class)
public class FeedControllerUnitTest {

	@MockBean
	FeedService feedService;

	@MockBean
	JwtAuthFilter authFilter;

	@MockBean
	JwtUtils jwtUtils;

	@Autowired
	MockMvc mockMvc;

	BlogPost blogPost;
	User user;
	Comment comment;

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

		blogPost = new BlogPost();
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

		comment = new Comment();
		comment.setBlogPost(blogPost);
		comment.setContent("fake-comment");
		comment.setFunnyCount(0L);
		comment.setId(1L);
		comment.setLikeCount(100L);
		comment.setLoveCount(0L);
		comment.setReactedUsers(new HashSet<>());
		comment.setUser(user);
		blogPost.setComments(new ArrayList<>(List.of(comment)));

	}

	@Test
	void testTimeline() throws Exception {

		FeedItem item = FeedItem.convertToFeedItem(blogPost);

		when(feedService.timeline(0, 10)).thenReturn(List.of(item));

		mockMvc.perform(MockMvcRequestBuilders.get("/timeline").param("start", "0").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].likeCount").value(blogPost.getLikes()))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].blogDescription").value(blogPost.getTitle()))

		;

	}

}
