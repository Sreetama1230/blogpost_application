package com.example.demo.controller;

import com.example.demo.dao.CategoryDao;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.exception.*;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Mock
    CategoryService categoryService;

    @InjectMocks
    CategoryController categoryController;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;
    Category c;
    User user;
    @BeforeEach
    void setUp(){
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new CategoryExceptionHandler(),new CategoryLinkedToBlogsHandler())
                .build();

        c = new Category("#test", new HashSet<BlogPost>());
        c.setId(1L);

        user = new User();
        user.setUsername("fake-username");
        user.setBio("fake-bio");
        user.setEmail("fake@ppp.com");
        user.setId(1L);

    }

    @Test
    void testCreateCategory() throws Exception {

        CategoryDTO categoryDTO = new CategoryDTO("#fake-name");
       Category cg = new Category(categoryDTO.getName(), new HashSet<BlogPost>());

     when(categoryService.getByName(categoryDTO.getName()))
             .thenThrow(new ResourceNotFoundException("Category with provided name is not present"));

      when(categoryService.createCategory(any(Category.class))).thenReturn(cg);


        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(categoryDTO))
        ).andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value(categoryDTO.getName()));


        verify(categoryService).createCategory(any(Category.class));

    }

    @Test
    void testCreateCategory_AlreadyPresent() throws Exception {

        CategoryDTO categoryDTO = new CategoryDTO("#test");

        when(categoryService.getByName(categoryDTO.getName()))
                .thenReturn(c);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO))
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(result -> {
                    Throwable ex = result.getResolvedException();
                    assert ex instanceof CategoryException;
                    assert ex.getMessage().equals("Category is already present with the provided name...");
                });

    //when the category already exists, createCategory() should not be called.
        verify(categoryService,never()).createCategory(any());

    }
    @Test
    void testGetAll() throws Exception {
           Category anotherCategory = new Category("test2",new HashSet<>());
           anotherCategory.setId(2L);
            when(categoryService.getAll()).thenReturn(List.of(c,anotherCategory));

            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                            .andExpect( jsonPath("$",hasSize(2)))
                                    .andExpect( jsonPath("$[0].name").value("#test"))
                    .andExpect( jsonPath("$[1].name").value("test2"));
            verify(categoryService).getAll();

    }
    @Test
    void testListBlogsByCategory() throws Exception {
        BlogPost blogPost1 = new BlogPost();
        blogPost1.setId(1L);
        blogPost1.setTitle("fake-title-1");
        blogPost1.setContent("fake-content-1");
        blogPost1.setCategories(new HashSet<>(List.of(c)));
        blogPost1.setAuthor(user);

        BlogPost blogPost2 = new BlogPost();
        blogPost2.setId(2L);
        blogPost2.setTitle("fake-title-2");
        blogPost2.setContent("fake-content-2");
        blogPost2.setCategories(new HashSet<>(List.of(c)));
        blogPost2.setAuthor(user);

        user.setBlogPosts(List.of(blogPost1,blogPost2));
        c.setBlogPosts(new HashSet<>(List.of(blogPost1, blogPost2)));

        List<BlogPostResponse> list = new ArrayList<>();
        list.add(BlogPostResponse.convertBlogPostRespons(blogPost1));
        list.add(BlogPostResponse.convertBlogPostRespons(blogPost2));

//        when(categoryDao.findByName(c.getName())).thenReturn(Optional.of(c));
        when(categoryService.listBlogsByCategory(c.getName())).thenReturn(list);

        mockMvc.perform(get("/categories/name").param("name","#test"))
                .andExpect(status().isOk())
                .andExpect( jsonPath("$",hasSize(2)))
                .andExpect( jsonPath("$[0].title").value("fake-title-1"))
                .andExpect( jsonPath("$[1].title").value("fake-title-2"));
        verify(categoryService).listBlogsByCategory("#test");
    }

    @Test
    void testDeleteById() throws Exception {

        when(categoryService.deleteById(1L)).thenReturn(c);
//        String s=  mockMvc.perform(delete("/categories/1")).andReturn().getResponse().getContentAsString();
//      System.out.println(s);
        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(c.getName()));

        verify(categoryService).deleteById(1L);


    }

    @Test
    void testDeleteById_BlogsLinkedWithCategory() throws Exception {


        BlogPost blogPost1 = new BlogPost();
        blogPost1.setId(1L);
        blogPost1.setTitle("fake-title-1");
        blogPost1.setContent("fake-content-1");
        blogPost1.setCategories(new HashSet<>(List.of(c)));
        blogPost1.setAuthor(user);


        user.setBlogPosts(List.of(blogPost1));
        c.setBlogPosts(new HashSet<>(List.of(blogPost1)));
        c.setId(1L);

        when(categoryService.deleteById(1L))
                .thenThrow(new CategoryLinkedToBlogs("Some Blogs are linked with this category!..can not be deleted!"));

        mockMvc.perform(delete("/categories/1")

                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("Some Blogs are linked with this category!..can not be deleted!"))
                .andExpect(result -> {
                    Throwable ex = result.getResolvedException();
                    assert ex instanceof CategoryLinkedToBlogs;
                    assert ex.getMessage().equals("Some Blogs are linked with this category!..can not be deleted!");
                });
        verify(categoryService).deleteById(1L);
    }
}
