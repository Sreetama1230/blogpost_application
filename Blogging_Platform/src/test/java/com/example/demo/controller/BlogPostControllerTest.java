package com.example.demo.controller;


import com.example.demo.dto.BlogPostDTO;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.BlogPostUserResponse;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BlogPostControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BlogPostService blogPostService;



    @InjectMocks
    private BlogPostController blogPostController;

    @BeforeEach
    void setUp(){
        mockMvc= MockMvcBuilders.standaloneSetup(blogPostController).build();

    }

    @Test
    void createBlogPost() throws Exception {
        BlogPostDTO request = new BlogPostDTO();
        request.setTitle("Test Blog");
        request.setContent("Test Content");

        BlogPostResponse response = new BlogPostResponse();
        response.setId(1L);
        response.setTitle("Test Blog");
        response.setContent("Test Content");

        when(blogPostService.createOrUpdateBlogPost(any(BlogPostDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Blog"))
                .andExpect(jsonPath("$.content").value("Test Content"));

        verify(blogPostService).createOrUpdateBlogPost(any(BlogPostDTO.class));
    }
    @Test
    void getBlogsByTitleAndUserId() throws Exception {

        BlogPostUserResponse user;
        user = new BlogPostUserResponse();
        user.setId(1L);
        user.setUsername("fake-username");
        user.setEmail("fake-email");


        List<BlogPostResponse> list = List.of(
                new BlogPostResponse("Test Title - 1","Content 1", LocalDateTime.now(),user),
                new BlogPostResponse("Test Title - 2","Content 2",LocalDateTime.now(),user)

        );

        when(blogPostService.getBlogsByTitleAndUserId("test",1L))
                .thenReturn(list);

        mockMvc.perform(get("/blogs/title/test/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Test Title - 1"))
                .andExpect(jsonPath("$[1].title").value("Test Title - 2"));
        verify(blogPostService).getBlogsByTitleAndUserId("test",1L);



    }

    @Test
    void updateBlogPost() throws Exception {
        BlogPostDTO request = new BlogPostDTO();
        request.setTitle("Test New Blog");
        request.setId(1L);
        request.setContent("Test New Content");


        BlogPostResponse response = new BlogPostResponse();
        response.setId(1L);
        response.setTitle("Test New Blog");
        response.setContent("Test New Content");


        when(blogPostService.createOrUpdateBlogPost(any(BlogPostDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test New Blog"))
                .andExpect(jsonPath("$.content").value("Test New Content"));

        verify(blogPostService).createOrUpdateBlogPost(any(BlogPostDTO.class));
    }
    @Test
    void testDeleteBlogPost() throws Exception {

        BlogPostResponse response = new BlogPostResponse();
        response.setId(1L);
        response.setTitle("Test Blog");
        response.setContent("Test Content");

        when(blogPostService.deleteBlogPost(1L)).thenReturn(response);

        mockMvc.perform(delete("/blogs/1")
                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Blog"))
                .andExpect(jsonPath("$.content").value("Test Content"));

        verify(blogPostService).deleteBlogPost(1L);
    }
}
