package com.example.demo.service;


import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.CommentDTO;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentDao commentDao;
    @Mock
    private BlogPostDao blogPostDao;
    @Mock
    private UserDao userDao;
    @InjectMocks
    private CommentService commentService;

    private User user;
    private User user1;
    private BlogPost blogPost;
    private Comment comment;

    private CommentDTO dto;
    @BeforeEach
    void setUp(){


        user = new User();
        user.setId(1L);
        user.setUsername("fake-username");
        user.setEmail("fake-email");


      //auth user
        user1 = new User();
        user1.setId(2L);
        user1.setUsername("fake-username-2");
        user1.setEmail("fake-email-2");

        blogPost = new BlogPost();
        blogPost.setId(1L);
        blogPost.setTitle("fake-title");
        blogPost.setContent("fake-content");
        blogPost.setComments(new ArrayList<>());
        blogPost.setAuthor(user);


        comment = new Comment();
        comment.setContent("fake-comment");
        comment.setId(1L);
        comment.setUser(user);
        comment.setBlogPost(blogPost);

        user.setBlogPosts(new ArrayList<>(List.of(blogPost)));
        user.setComments(new ArrayList<>(List.of(comment)));
        blogPost.setComments(new ArrayList<>(List.of(comment)));

    }

    //update
    @Test
    void testUpdateComment_Success(){

        dto = new CommentDTO();
        dto.setId(1L);
        dto.setContent("fake-new-comment");

        try(MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)){
            //user1 logged in
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
            when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
            when(userDao.findById(2L)).thenReturn(Optional.of(user1));
            when(blogPostDao.save(any(BlogPost.class))).thenReturn(blogPost);
            when(userDao.save(any(User.class))).thenReturn(user);

            mockedStatic.when(()->SecurityUtils.isUser(user)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isEditor(user)).thenReturn(true);

            mockedStatic.when(()->SecurityUtils.isEditor(user1)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isUser(user1)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isAdmin(user1)).thenReturn(true);

            mockStatic(CommentService.class).when(()->CommentService.canUpdateOrDelete(user1,user)).thenReturn(true);

            BlogPostResponse blogPostResponse
                    = commentService.createOrUpdateComment(dto,1L);
            assertNotNull(blogPostResponse);

            assertEquals(1,blogPostResponse.getComments().size());
            assertEquals("fake-new-comment(edited)",blogPostResponse.getComments().get(0).getContent());

        }
    }


    @Test
    void testCreateComment_Success(){

        dto = new CommentDTO();
        dto.setContent("fake-new-comment");

        try(MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)){
            //user1 logged in
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
            when(userDao.findById(2L)).thenReturn(Optional.of(user1));
            when(blogPostDao.save(any(BlogPost.class))).thenReturn(blogPost);


            mockedStatic.when(()->SecurityUtils.isUser(user)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isEditor(user)).thenReturn(true);

            mockedStatic.when(()->SecurityUtils.isEditor(user1)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isUser(user1)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isAdmin(user1)).thenReturn(true);

            int comments =blogPost.getComments().size();
            //should create new comment
            BlogPostResponse blogPostResponse
                    = commentService.createOrUpdateComment(dto,1L);
            assertNotNull(blogPostResponse);

            assertEquals(comments+1,blogPostResponse.getComments().size());
            assertEquals("fake-new-comment",blogPostResponse.getComments().get(blogPostResponse.getComments().size()-1).getContent());

        }
    }
        @Test
        void testUpdateComment_NotFound(){
            dto = new CommentDTO();
            dto.setId(2L);
            dto.setContent("fake-new-comment");

            try(MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)){
                //user1 logged in
                mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
                when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));

                when(commentDao.findById(2L)).thenReturn(Optional.empty());
                when(userDao.findById(2L)).thenReturn(Optional.of(user1));



                mockedStatic.when(()->SecurityUtils.isUser(user)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isEditor(user)).thenReturn(true);

                mockedStatic.when(()->SecurityUtils.isEditor(user1)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isUser(user1)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isAdmin(user1)).thenReturn(true);


                assertThrows(ResourceNotFoundException.class,()->commentService.createOrUpdateComment(dto,1L));


        }

    }

    @Test
    void testUpdateComment_DontHavePermission() {
        dto = new CommentDTO();
        dto.setId(1L);
        dto.setContent("fake-new-comment");

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            //user1 logged in
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
            when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
            when(userDao.findById(2L)).thenReturn(Optional.of(user1));


            mockedStatic.when(() -> SecurityUtils.isUser(user)).thenReturn(false);
            mockedStatic.when(() -> SecurityUtils.isAdmin(user)).thenReturn(false);
            mockedStatic.when(() -> SecurityUtils.isEditor(user)).thenReturn(true);

            mockedStatic.when(() -> SecurityUtils.isEditor(user1)).thenReturn(true);
            mockedStatic.when(() -> SecurityUtils.isUser(user1)).thenReturn(false);
            mockedStatic.when(() -> SecurityUtils.isAdmin(user1)).thenReturn(false);

            mockStatic(CommentService.class).when(() -> CommentService.canUpdateOrDelete(user1, user)).thenReturn(false);

            assertThrows(DoNotHavePermissionError.class, () -> commentService.createOrUpdateComment(dto, 1L));


        }
    }
        @Test
        void testDeleteComment_Success(){


            try(MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)){
                //user1 logged in
                mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
                when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
                when(userDao.findById(2L)).thenReturn(Optional.of(user1));
                when(userDao.findById(1L)).thenReturn(Optional.of(user));
                when(commentDao.findCommentByIdAndBlogPostId(1L,1L)).thenReturn(Optional.of(comment));

                mockedStatic.when(()->SecurityUtils.isUser(user)).thenReturn(true);
                mockedStatic.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isEditor(user)).thenReturn(false);

                mockedStatic.when(()->SecurityUtils.isEditor(user1)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isUser(user1)).thenReturn(false);
                mockedStatic.when(()->SecurityUtils.isAdmin(user1)).thenReturn(true);

                mockStatic(CommentService.class).when(()->CommentService.canUpdateOrDelete(user1,user)).thenReturn(true);

                int countCom=    blogPost.getComments().size();
                CommentResponse commentResponse
                        = commentService.deleteComment(1L,1);


                assertEquals(Optional.of(1L).get(),commentResponse.getId());
                assertEquals(countCom-1,blogPost.getComments().size());
            }

    }
    @Test
    void testDeleteComment_Failure(){


        try(MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)){
            //user1 logged in
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(userDao.findById(2L)).thenReturn(Optional.of(user1));
            when(userDao.findById(1L)).thenReturn(Optional.of(user));
            when(commentDao.findCommentByIdAndBlogPostId(1L,1L)).thenReturn(Optional.of(comment));

            mockedStatic.when(()->SecurityUtils.isUser(user)).thenReturn(true);
            mockedStatic.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isEditor(user)).thenReturn(false);

            mockedStatic.when(()->SecurityUtils.isEditor(user1)).thenReturn(true);
            mockedStatic.when(()->SecurityUtils.isUser(user1)).thenReturn(false);
            mockedStatic.when(()->SecurityUtils.isAdmin(user1)).thenReturn(false);

            mockStatic(CommentService.class).when(()->CommentService.canUpdateOrDelete(user1,user)).thenReturn(false);


            assertThrows(DoNotHavePermissionError.class,()->commentService.deleteComment(1L,1L));
        }

    }

    @Test
    void testGetById_Success(){
        when(commentDao.findById(1L)).thenReturn(Optional.of(comment));
        CommentResponse fetchedComment = commentService.getById(1L);
        assertEquals("fake-comment",fetchedComment.getContent());
    }

    @Test
    void testGetById_Failure(){
        when(commentDao.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->commentService.getById(99L));

    }
}
