package com.example.demo.controller.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.demo.config.JwtAuthFilter;
import com.example.demo.config.JwtUtils;
import com.example.demo.controller.CategoryController;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.exception.CategoryLinkedToBlogs;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.CategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CategoryController.class)
public class CategoryControllerUnitTest {

	@MockBean
	CategoryService categoryService;

	@MockBean
	JwtAuthFilter authFilter;

	@MockBean
	JwtUtils jwtUtils;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	MockMvc mockMvc;

	BlogPost blogPost;
	User user;
	Category category;

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
	public void testCreateCategory() throws JsonProcessingException, Exception {

		category = new Category();
		category.setName("#new-test-category");
		category.setId(2L);

		CategoryDTO categoryDto = new CategoryDTO();
		categoryDto.setName(category.getName());

		when(categoryService.getByName(category.getName()))
				.thenThrow(new ResourceNotFoundException("Category with provided name is not present"));

		when(categoryService.createCategory(any(Category.class))).thenReturn(category);

		mockMvc.perform(MockMvcRequestBuilders.post("/category").content(objectMapper.writeValueAsString(categoryDto))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andDo(print())
				.andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.name").value("#new-test-category"));

		verify(categoryService).createCategory(any(Category.class));
	}

	@Test
	public void testCreateCategory_WithSameName_FaiureWithCategoryException()
			throws JsonProcessingException, Exception {

		category = new Category();
		category.setName("#new-test-category");
		category.setId(2L);

		CategoryDTO categoryDto = new CategoryDTO();
		categoryDto.setName(category.getName());

		when(categoryService.getByName(category.getName())).thenReturn(category);


		mockMvc.perform(MockMvcRequestBuilders.post("/category").content(objectMapper.writeValueAsString(categoryDto))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andDo(print())
				.andExpect(jsonPath("$.msg").value("Category is already present with the provided name..."));

		 verify(categoryService).getByName("#new-test-category");

		    verify(categoryService, never())
		            .createCategory(any(Category.class));
	}

	@Test
	public void testGetAll() throws Exception {
		category = new Category();
		category.setName("#test-category");
		category.setId(2L);

		when(categoryService.getAll()).thenReturn(List.of(category));

		mockMvc.perform(MockMvcRequestBuilders.get("/category")).andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$[0].id").value(2L)).andExpect(jsonPath("$[0].name").value("#test-category"));

		verify(categoryService).getAll();

	}

	@Test
	public void testListBlogsByCategory() throws Exception {

		category = new Category();
		category.setName("#test-category");
		category.setId(2L);
		category.setBlogPosts(new HashSet<>(Set.of(blogPost)));

		BlogPostResponse blogPostResponse = BlogPostResponse.convertBlogPostRespons(blogPost);

		when(categoryService.listBlogsByCategory(category.getName())).thenReturn(List.of(blogPostResponse));

		mockMvc.perform(MockMvcRequestBuilders.get("/category/name").param("name", "#test-category"))
				.andExpect(status().isOk()).andDo(print()).andExpect(jsonPath("$[0].id").value(1L));

		verify(categoryService).listBlogsByCategory("#test-category");
	}
	
	
	@Test
	public void testListBlogsByCategory_WithWrongId_FailureWithResourceNotFoundException() throws Exception {
		category = new Category();
		category.setName("#test-category");
		category.setId(2L);

		when(categoryService.listBlogsByCategory("#sample")).thenThrow(new ResourceNotFoundException("Category with provided name is not present."));

		mockMvc.perform(MockMvcRequestBuilders.get("/category/name").param("name", "#sample"))
		
		.andExpect(status().isNotFound()).andDo(print())
				.andExpect(jsonPath("$.msg").value("Category with provided name is not present."));

		verify(categoryService).listBlogsByCategory("#sample");

	}

	@Test
	public void testDelete() throws Exception {
		category = new Category();
		category.setName("#test-category");
		category.setId(2L);

		when(categoryService.deleteById(2L)).thenReturn(category);

		mockMvc.perform((MockMvcRequestBuilders.delete("/category/2"))).andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$.id").value(2));

		verify(categoryService).deleteById(2L);

	}
	
	@Test
	public void testDelete_WithWrongId_FailureWithResourceNotFoundException() throws Exception {
		category = new Category();
		category.setName("#test-category");
		category.setId(2L);

		when(categoryService.deleteById(2L)).thenThrow(new ResourceNotFoundException("Category with provided name is not present."));

		mockMvc.perform((MockMvcRequestBuilders.delete("/category/2"))).andExpect(status().isNotFound()).andDo(print())
				.andExpect(jsonPath("$.msg").value("Category with provided name is not present."));

		verify(categoryService).deleteById(2L);

	}
	
	@Test
	public void testDelete_WithWrongId_FailureWithCategoryLinkedToBlogs() throws Exception {
		category = new Category();
		category.setName("#test-category");
		category.setId(2L);

		when(categoryService.deleteById(2L)).thenThrow(new CategoryLinkedToBlogs("Some Blogs are linked with this category!..can not be deleted!"));

		mockMvc.perform((MockMvcRequestBuilders.delete("/category/2"))).andExpect(status().isBadRequest()).andDo(print())
				.andExpect(jsonPath("$.msg").value("Some Blogs are linked with this category!..can not be deleted!"));

		verify(categoryService).deleteById(2L);

	}
	

}
