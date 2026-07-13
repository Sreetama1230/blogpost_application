package com.example.demo.service.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.demo.dao.CategoryDao;
import com.example.demo.dao.EventDao;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.CategoryLinkedToBlogs;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.service.CategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
public class CategoryServiceUnitTest {

	@Mock
	private CategoryDao categoryDao;

	@InjectMocks
	private CategoryService categoryService;

	@Mock
	private EventDao eventDao;
	@Mock
	private ObjectMapper objectMapper;

	User user;
	BlogPost blogPost;
	Comment comment;
	Set<Category> categories;
	List<Category> listCategory = new ArrayList<>();

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
		set.add("ROLE_ADMIN");
		user.setRoles(set);
		blogPost = new BlogPost();
		blogPost.setId(1L);
		blogPost.setContent("blog content");
		blogPost.setTitle("blog title");
		blogPost.setAuthor(user);
		categories = new HashSet<>();
		HashSet<BlogPost> blogPosts = new HashSet<>();
		blogPosts.add(blogPost);
		categories.add(new Category("fake-category", blogPosts));
		blogPost.setCategories(categories);

		comment = new Comment();
		comment.setId(1L);
		comment.setContent("test comment");
		comment.setLikeCount(1L);
		comment.setFunnyCount(1L);
		comment.setLoveCount(1L);

		blogPost.setComments(new ArrayList<>(List.of(comment)));
		comment.setBlogPost(blogPost);

		List<BlogPost> listofUserBlogPosts = new ArrayList<>();
		listofUserBlogPosts.add(blogPost);
		user.setBlogPosts(listofUserBlogPosts);

		for (Category category : categories) {
			listCategory.add(category);
		}

	}

	@Test
	public void testGetAll() {

		when(categoryDao.findAll()).thenReturn(listCategory);

		List<Category> actualCategories = categoryService.getAll();

		assertEquals(listCategory.size(), actualCategories.size());
		assertEquals(listCategory.get(0).getName(), actualCategories.get(0).getName());

		verify(categoryDao).findAll();
	}

	@Test
	public void testGetById() {

		Category expectedCategory = listCategory.get(0);

		when(categoryDao.findById(1L)).thenReturn(Optional.of(expectedCategory));

		Category actualCategory = categoryService.getById(1L);

		assertEquals(expectedCategory.getBlogPosts().size(), actualCategory.getBlogPosts().size());
		assertEquals(expectedCategory.getName(), actualCategory.getName());
		assertEquals(expectedCategory.getId(), actualCategory.getId());

		verify(categoryDao, times(2)).findById(1L);

	}

	@Test
	public void testGetById_WithInvalidCategoryId_FailureWithResourceNotFoundException() {

		when(categoryDao.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			categoryService.getById(1L);
		});
		assertNotNull(resourceNotFoundException);
		assertEquals("Category with provided id is not present.", resourceNotFoundException.getMessage());

	}

	@Test
	public void testGetByName_WithInvalidCategoryName_FailureWithResourceNotFoundException() {

		when(categoryDao.findByName("fake-category")).thenReturn(Optional.empty());

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			categoryService.getByName("fake-category");
		});
		assertNotNull(resourceNotFoundException);
		assertEquals("Category with provided name is not present", resourceNotFoundException.getMessage());

	}

	@Test
	public void testGetByName() {

		Category expectedCategory = listCategory.stream().filter(c -> c.getName().equals("fake-category"))
				.collect(Collectors.toList()).get(0);

		when(categoryDao.findByName("fake-category")).thenReturn(Optional.of(expectedCategory));

		Category actualCategory = categoryService.getByName("fake-category");

		assertEquals(expectedCategory.getBlogPosts().size(), actualCategory.getBlogPosts().size());
		assertEquals(expectedCategory.getName(), actualCategory.getName());
		assertEquals(expectedCategory.getId(), actualCategory.getId());

		verify(categoryDao, times(2)).findByName("fake-category");

	}

	@Test
	public void testCreateCategory() throws JsonProcessingException {

		Category newCategory = new Category("#test", new HashSet<>());
		when(categoryDao.save(newCategory)).thenReturn(newCategory);

		Category savedCategory = categoryService.createCategory(newCategory);

		assertEquals(newCategory.getId(), savedCategory.getId());
		assertEquals(newCategory.getName(), savedCategory.getName());

		String payload = objectMapper.writeValueAsString(newCategory);

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		verify(eventDao).save(captor.capture());

		Event event = captor.getValue();

		assertEquals(EventType.CREATE, event.getEventType());
		assertEquals(TransactionType.CATEGORY, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(savedCategory.getId() + "", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

	}

	@Test
	public void testDeleteById() throws JsonProcessingException {

		Category newCategory = new Category("#test", new HashSet<>());
		newCategory.setId(5L);

		when(categoryDao.findById(5L)).thenReturn(Optional.of(newCategory));

		Category deletedCategory = categoryService.deleteById(5L);

		assertEquals(newCategory.getId(), deletedCategory.getId());
		assertEquals(newCategory.getName(), deletedCategory.getName());

		verify(categoryDao).deleteById(5L);
		
		String payload = objectMapper.writeValueAsString(deletedCategory.getId());

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		verify(eventDao).save(captor.capture());

		Event event = captor.getValue();

		assertEquals(EventType.DELETE, event.getEventType());
		assertEquals(TransactionType.CATEGORY, event.getTransactionType());
		assertEquals(EventStatus.PENDING, event.getStatus());
		assertEquals(deletedCategory.getId() + "", event.getTransactionId());
		assertEquals(0, event.getRetryCount());
		assertEquals(payload, event.getPayload());

	}

	@Test
	public void testDelete_FailureWithCategoryLinkedToBlogs() {

		Category newCategory = new Category("#test", new HashSet<>(List.of(blogPost)));
		newCategory.setId(5L);

		when(categoryDao.findById(5L)).thenReturn(Optional.of(newCategory));

		CategoryLinkedToBlogs categoryLinkedToBlogs = assertThrows(CategoryLinkedToBlogs.class, () -> {
			categoryService.deleteById(5L);
		});

		assertNotNull(categoryLinkedToBlogs);
		assertEquals("Some Blogs are linked with this category!..can not be deleted!",
				categoryLinkedToBlogs.getMessage());

	}

	@Test
	public void testDelete_WithInvalidId_FailureWithhResourceNotFoundException() {

		when(categoryDao.findById(5L)).thenReturn(Optional.empty());

		ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class, () -> {
			categoryService.deleteById(5L);
		});

		assertNotNull(resourceNotFoundException);
		assertEquals("Category with provided id is not present.", resourceNotFoundException.getMessage());

	}

}
