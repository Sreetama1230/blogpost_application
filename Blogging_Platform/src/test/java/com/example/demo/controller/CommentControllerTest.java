package com.example.demo.controller;


import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.CommentDTO;
import com.example.demo.exception.CategoryException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ResourceNotFoundExceptionHandler;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;
import com.example.demo.service.CommentService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {

    @InjectMocks
    private CommentController commentController;

    @Mock
    private CommentService commentService;

    @Mock
    private UserDao userDao;
    @Mock
    private BlogPostDao blogPostDao;

    private ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){
    mockMvc = MockMvcBuilders
            .standaloneSetup(commentController)
            .setControllerAdvice(new ResourceNotFoundExceptionHandler())
            .build();
    }

    @Test
    void testAddComments() throws Exception {
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent("fake-comment");

        User user = new User();
        user.setId(1L);
        user.setUsername("fake-username");
        user.setEmail("fake@gmail.com");

        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("fake-title");
        blogPost.setId(1L);
        blogPost.setAuthor(user);

        Comment c = new Comment();
        c.setBlogPost(blogPost);
        c.setUser(user);
        c.setContent(commentDTO.getContent());
        c.setId(1L);
        c.setCreateAt(LocalDateTime.now());

        blogPost.setComments(List.of(c));

        mockStatic(SecurityUtils.class).when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(commentService.createOrUpdateComment(any(CommentDTO.class),eq(1L)))
                .thenReturn(BlogPostResponse.convertBlogPostRespons(blogPost));

//        String s=  mockMvc.perform(  post("/comments").param("blogPostId","1")
//
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(commentDTO))
//
//                )
//                .andReturn().getResponse().getContentAsString();
//      System.out.println("S: "+s);

        mockMvc.perform(
                post("/comments").param("blogPostId","1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDTO))
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(blogPost.getId()))

                .andExpect(jsonPath("$.title").value(blogPost.getTitle())) ;

        verify(commentService).createOrUpdateComment(any(CommentDTO.class),eq(1L));


    }

   @Test
    void testUpdateComments() throws Exception {
       CommentDTO commentDTO = new CommentDTO();
       commentDTO.setContent("update-fake-comment");
       commentDTO.setId(1L);

       User user = new User();
       user.setId(1L);
       user.setUsername("fake-username");
       user.setEmail("fake@gmail.com");

       BlogPost blogPost = new BlogPost();
       blogPost.setTitle("fake-title");
       blogPost.setId(1L);
       blogPost.setAuthor(user);

       Comment c = new Comment();
       c.setBlogPost(blogPost);
       c.setUser(user);
       c.setContent(commentDTO.getContent());
       c.setId(1L);
       c.setCreateAt(LocalDateTime.now());

       blogPost.setComments(List.of(c));

       mockStatic(SecurityUtils.class).when(SecurityUtils::getCurrentUserId).thenReturn(1L);
       when(commentService.createOrUpdateComment(any(CommentDTO.class),eq(1L)))
               .thenReturn(BlogPostResponse.convertBlogPostRespons(blogPost));


       mockMvc.perform(
                       put("/comments").param("blogPostId","1")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(objectMapper.writeValueAsString(commentDTO))
               ).andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(blogPost.getId()))

               .andExpect(jsonPath("$.title").value(blogPost.getTitle())) ;

       verify(commentService).createOrUpdateComment(any(CommentDTO.class),eq(1L));

    }
    @Test
    void testGetById() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("fake-username");
        user.setEmail("fake@gmail.com");

        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("fake-title");
        blogPost.setId(1L);
        blogPost.setAuthor(user);

        Comment c = new Comment();
        c.setBlogPost(blogPost);
        c.setUser(user);
        c.setContent("fake-content");
        c.setId(1L);
        c.setCreateAt(LocalDateTime.now());

        blogPost.setComments(List.of(c));

        when(commentService.getById(1L)).thenReturn(CommentResponse.convertCommentResponse(c));

        mockMvc.
                perform(get("/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()));
        verify(commentService).getById(1L);

    }

    @Test
    void testGetById_NotFound() throws Exception {

        when(commentService.getById(1L))
                .thenThrow(new ResourceNotFoundException("Resource is not present!"));

        mockMvc.
                perform(get("/comments/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(result -> {
                    Throwable ex = result.getResolvedException();
                    assert ex instanceof ResourceNotFoundException;
                    assert ex.getMessage().equals("Resource is not present!");
                });
        verify(commentService).getById(1L);

    }

    @Test
    void testDeleteComment() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("fake-username");
        user.setEmail("fake@gmail.com");

        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("fake-title");
        blogPost.setId(1L);
        blogPost.setAuthor(user);

        Comment c = new Comment();
        c.setBlogPost(blogPost);
        c.setUser(user);
        c.setContent("fake-content");
        c.setId(1L);
        c.setCreateAt(LocalDateTime.now());

        blogPost.setComments(List.of(c));

        when(commentService.deleteComment(1L,1L)).thenReturn(CommentResponse.convertCommentResponse(c));

        mockMvc.
                perform(delete("/comments/1/blogposts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()));
        verify(commentService).deleteComment(1L,1L);
    }
}
