package com.example.demo.service;

import com.example.demo.dao.CategoryDao;
import com.example.demo.exception.CategoryLinkedToBlogs;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryDao categoryDao;
    @InjectMocks
    CategoryService categoryService;

    private User user;
    private User user1;
    private BlogPost blogPost;
    private Category category;
    HashSet<BlogPost> set = new HashSet<>();
    @BeforeEach
    void setUp(){

        user = new User();
        user.setId(1L);
        user.setUsername("fake-username");
        user.setEmail("fake-email");



        user1 = new User();
        user1.setId(2L);
        user1.setUsername("fake-username-2");
        user1.setEmail("fake-email-2");


        category = new Category();
        category.setId(1L);
        category.setName("#fake-name");

        blogPost = new BlogPost();
        blogPost.setId(1L);
        blogPost.setTitle("fake-title");
        blogPost.setContent("fake-content");
        blogPost.setComments(new ArrayList<>());
        blogPost.setAuthor(user);
        blogPost.setCategories(Set.of(category));



        set.add(blogPost);
       // category.setBlogPosts(set);
        user.setBlogPosts(new ArrayList<>(List.of(blogPost)));

    }

    @Test
    void testGetAll(){
       List<Category> categories = new ArrayList<>();
       categories.add(category);
       when(categoryDao.findAll()).thenReturn(categories);

      List<Category> fetchedCategory= categoryService.getAll();
      fetchedCategory = categoryService.getAll();
      assertNotNull(fetchedCategory);
      assertEquals(categories.size(),fetchedCategory.size());

    }
    @Test
    void testGetById(){

        when(categoryDao.findById(1L)).thenReturn( Optional.of(category));

       Category c= categoryService.getById(1L);
       assertEquals(category.getName(),c.getName());

    }
    @Test
    void testGetById_NotFound(){
        when(categoryDao.findById(122L)).thenReturn( Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->categoryService.getById(122L));
    }
    @Test
    void testGetByName(){
        when(categoryDao.findByName("#fake-name")).thenReturn( Optional.of(category));

        Category c= categoryService.getByName("#fake-name");
        assertEquals(category.getName(),c.getName());
    }
    @Test
    void testGetByName_NotFound(){
        when(categoryDao.findByName("dummy")).thenReturn( Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->categoryService.getByName("dummy"));
    }
    @Test
    void testCreateCategory(){
        when(categoryDao.save(any(Category.class))).thenReturn(category);
        Category c = categoryService.createCategory(category);
        assertNotNull(c);
        assertEquals("#fake-name",c.getName());

    }
    @Test
    void testDeleteById(){
        when(categoryDao.findById(1L)).thenReturn(Optional.of(category));
       Category c= categoryService.deleteById(1L);
       assertNotNull(c);
        assertEquals(category.getName(),c.getName());
    }

    @Test
    void testDeleteById_CategoryLinkedToBlogs(){
        category.setBlogPosts(set);
        when(categoryDao.findById(1L)).thenReturn(Optional.of(category));
        assertThrows(CategoryLinkedToBlogs.class,()->categoryService.deleteById(1L));
    }
    @Test
    void testListBlogsByCategory(){
        List<Category> categories = new ArrayList<>();
        categories.add(category);
        when(categoryDao.findByName("#fake-name")).thenReturn(Optional.of(category));
       Set< BlogPost> dbBlogPostResponses = category.getBlogPosts();

        List<BlogPostResponse> fetchedCategory= categoryService.listBlogsByCategory("#fake-name");

        assertNotNull(fetchedCategory);
        assertEquals(dbBlogPostResponses.size(),fetchedCategory.size());
    }

}
