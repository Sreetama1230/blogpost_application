package com.example.demo.controller.unit;

import java.awt.PageAttributes.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.config.JwtAuthFilter;
import com.example.demo.config.JwtUtils;
import com.example.demo.controller.BlogPostController;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.exception.CategoryException;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.BlogPostService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(BlogPostController.class)
public class BlogPostControllerUnitTest {

	@MockBean
	BlogPostService blogPostService;

	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	MockMvc mockMvc;


	@MockBean
	JwtAuthFilter authFilter;

	@MockBean
	JwtUtils jwtUtils;
	BlogPost blogPost;
	User user;

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

	}

	@Test
	public void testCreateBlogPost() throws JsonProcessingException, Exception {
		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle(blogPost.getTitle());

		Set<Category> categories = blogPost.getCategories();

		Set<CategoryDTO> categoryDTOs = categories.stream().map(c -> CategoryDTO.convertToCategoryDTO(c))
				.collect(Collectors.toSet());

		blogPostDTO.setCategories(categoryDTOs);
		blogPostDTO.setContent(blogPost.getContent());

		BlogPostResponse blogPostResponse = BlogPostResponse.convertBlogPostRespons(blogPost);
		when(blogPostService.createOrUpdateBlogPost(any(BlogPostDTO.class))).thenReturn(blogPostResponse);

		mockMvc.perform(MockMvcRequestBuilders.post("/blog").content(objectMapper.writeValueAsString(blogPostDTO))
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON))

				.andExpect(status().isCreated()).andDo(print()).andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.content").value("blog content"))
				.andExpect(jsonPath("$.title").value("blog title"))

		;

		verify(blogPostService).createOrUpdateBlogPost(any(BlogPostDTO.class));

	}

	@Test
	public void testCreateBlogPost_WithoutCategory_FailureWithCategoryException()
			throws JsonProcessingException, Exception {
		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle(blogPost.getTitle());
		blogPostDTO.setContent(blogPost.getContent());

		when(blogPostService.createOrUpdateBlogPost(any(BlogPostDTO.class))).thenThrow(

				new CategoryException("You have to specify a category to proceed"));

		mockMvc.perform(MockMvcRequestBuilders.post("/blog").content(objectMapper.writeValueAsString(blogPostDTO))
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON))

				.andExpect(status().isBadRequest()).andDo(print())
				.andExpect(jsonPath("$.msg").value("You have to specify a category to proceed"));

		verify(blogPostService).createOrUpdateBlogPost(any(BlogPostDTO.class));

	}

	@Test
	public void testUpdateBlogPost() throws JsonProcessingException, Exception {
		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle(blogPost.getTitle());

		Set<Category> categories = blogPost.getCategories();

		Set<CategoryDTO> categoryDTOs = categories.stream().map(c -> CategoryDTO.convertToCategoryDTO(c))
				.collect(Collectors.toSet());

		blogPostDTO.setCategories(categoryDTOs);
		blogPostDTO.setContent(blogPost.getContent());
		blogPostDTO.setId(blogPost.getId());

		BlogPostResponse blogPostResponse = BlogPostResponse.convertBlogPostRespons(blogPost);
		when(blogPostService.createOrUpdateBlogPost(any(BlogPostDTO.class))).thenReturn(blogPostResponse);

		mockMvc.perform(MockMvcRequestBuilders.put("/blog").content(objectMapper.writeValueAsString(blogPostDTO))
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON))

				.andExpect(status().isOk()).andDo(print()).andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.content").value("blog content"))
				.andExpect(jsonPath("$.title").value("blog title"))
				.andExpect(jsonPath("$.author.username").value("fake-username"));

		verify(blogPostService).createOrUpdateBlogPost(any(BlogPostDTO.class));

	}

	@Test
	public void testUpdateBlogPost_WithWrongUser_FailureWithDoNotHavePermissionError()
			throws JsonProcessingException, Exception {
		BlogPostDTO blogPostDTO = new BlogPostDTO();
		blogPostDTO.setTitle(blogPost.getTitle());
		blogPostDTO.setContent(blogPost.getContent());

		when(blogPostService.createOrUpdateBlogPost(any(BlogPostDTO.class))).thenThrow(

				new DoNotHavePermissionError("You can not do the update!"));

		mockMvc.perform(MockMvcRequestBuilders.post("/blog").content(objectMapper.writeValueAsString(blogPostDTO))
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON))

				.andExpect(status().isForbidden()).andDo(print())
				.andExpect(jsonPath("$.msg").value("You can not do the update!"));

		verify(blogPostService).createOrUpdateBlogPost(any(BlogPostDTO.class));

	}

	@Test
	public void testDeleteBlogPost() throws Exception {
		BlogPostResponse blogPostResponses = BlogPostResponse.convertBlogPostRespons(blogPost);
		when(blogPostService.deleteBlogPost(1L)).thenReturn(blogPostResponses);

		mockMvc.perform(MockMvcRequestBuilders.delete("/blog/1")).andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.title").value("blog title"))
				.andExpect(jsonPath("$.author.username").value("fake-username"));

		verify(blogPostService).deleteBlogPost(1L);
	}

	@Test
	public void testDeleteBlogPost_WithInvalidId_FailureWithResourceNotFoundException() throws Exception {

		when(blogPostService.deleteBlogPost(1L)).thenThrow(new ResourceNotFoundException("Resource is not found!"));

		mockMvc.perform(MockMvcRequestBuilders.delete("/blog/1")).andExpect(status().isNotFound()).andDo(print())
				.andExpect(jsonPath("$.msg").value("Resource is not found!"));

		verify(blogPostService).deleteBlogPost(1L);
	}
	
	@Test
	public void testDeleteBlogPost_WithWrongUser_FailureWithDoNotHavePermissionError()
			throws JsonProcessingException, Exception {
		

		when(blogPostService.deleteBlogPost(1L)).thenThrow(new DoNotHavePermissionError("You are not the author of this post or an admin!"));

		mockMvc.perform(MockMvcRequestBuilders.delete("/blog/1")).andExpect(status().isForbidden()).andDo(print())
				.andExpect(jsonPath("$.msg").value("You are not the author of this post or an admin!"));

		verify(blogPostService).deleteBlogPost(1L);

	}
	
	

	
	@Test
	public void testGetBlogsByTitleAndUserId_Success() throws Exception {

		BlogPostResponse blogPostResponses = BlogPostResponse.convertBlogPostRespons(blogPost);
		when(blogPostService.getBlogsByTitleAndUserId("blog title", 1L)).thenReturn(List.of(blogPostResponses));

		mockMvc.perform(MockMvcRequestBuilders.get("/blog/title/blog title/user/1")).andExpect(status().isOk())
				.andDo(print()).andExpect(jsonPath("$[0].id").value(1L))
				.andExpect(jsonPath("$[0].title").value("blog title"))
				.andExpect(jsonPath("$[0].author.username").value("fake-username"));
		verify(blogPostService).getBlogsByTitleAndUserId("blog title", 1L);

	}


}
