package com.example.demo.service;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
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

import org.mockito.MockedStatic;

import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;


@ExtendWith(MockitoExtension.class)
public class BlogPostServiceTest {


    @Mock
    private BlogPostDao blogPostDao;
    @Mock
    private UserDao userDao;

    @Mock
    private CommentDao commentDao;
    @Mock
    private UserService userService;
    @Mock
    private CategoryService categoryService;
    @InjectMocks
    private BlogPostService blogPostService;

    private User user;
    private User user1;
    private BlogPost blogPost;
    private Category category;


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
        category.setName("fake-category");
        category.setId(1L);


        blogPost = new BlogPost();
        blogPost.setId(1L);
        blogPost.setTitle("fake-title");
        blogPost.setContent("fake-content");
        blogPost.setComments(new ArrayList<>());
        blogPost.setAuthor(user);
        blogPost.setCategories(Set.of(category));


        HashSet<BlogPost> set = new HashSet<>();
        set.add(blogPost);
        category.setBlogPosts(set);
        user.setBlogPosts(new ArrayList<>(List.of(blogPost)));


    }
    @Test
    void testGetAll(){
        when(blogPostDao.findAll()).thenReturn(List.of(blogPost));
        List<BlogPost> bps = blogPostService.getAll();
        assertEquals(1,bps.size());
        assertEquals("fake-title",bps.get(0).getTitle());

    }
    @Test
    void testGetById_Success(){
        when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
        BlogPost post = blogPostService.getById(1L);
        assertEquals("fake-title",post.getTitle());
        assertEquals("fake-content",post.getContent());
    }
    @Test
    void testGetById_Failure(){
        when(blogPostDao.findById(99L)).thenReturn(Optional.empty());
       assertThrows(ResourceNotFoundException.class,()->blogPostService.getById(99L));
    }

    @Test
    void testCreateBlogPost_Success(){


        BlogPostDTO blogPostDTO = new BlogPostDTO();
        blogPostDTO.setTitle(blogPost.getTitle());
        blogPostDTO.setContent(blogPost.getContent());
        Set<CategoryDTO> dtos = new HashSet<>();
        for(Category c : blogPost.getCategories()){
            dtos.add(new CategoryDTO(c.getName()));
        }
        blogPostDTO.setCategories( dtos );

        try(MockedStatic<SecurityUtils>  utilities = mockStatic(SecurityUtils.class)){
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
            when(userService.getbyId(1L)).thenReturn(user);
            when(blogPostDao.save(any(BlogPost.class))).thenReturn(blogPost);
             BlogPostResponse blogPostResponse = blogPostService.createOrUpdateBlogPost(blogPostDTO);
             assertEquals(blogPost.getContent(),blogPostResponse.getContent());
             assertEquals(blogPost.getTitle(),blogPostResponse.getTitle());
             assertEquals(blogPost.getAuthor().getUsername(),blogPostResponse.getAuthor().getUsername());
            


        }
    }


    @Test
    void testUpdateBlogPost_Success(){


        BlogPostDTO blogPostDTO = new BlogPostDTO();
        blogPostDTO.setTitle("new-fake-title");
        blogPostDTO.setContent("new-fake-content");
        blogPostDTO.setId(1L);
        Set<CategoryDTO> dtos = new HashSet<>();
        for(Category c : blogPost.getCategories()){
            dtos.add(new CategoryDTO(c.getName()));
        }
        blogPostDTO.setCategories( dtos );

        try(MockedStatic<SecurityUtils>  utilities = mockStatic(SecurityUtils.class)){
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);
            when(userService.getbyId(1L)).thenReturn(user);

            //logged in user and blog author both are same


            mockStatic(BlogPostService.class).
                    when(()->BlogPostService.canUpdateOrDelete(user,user)).thenReturn(true);


            BlogPostResponse blogPostResponse = blogPostService.createOrUpdateBlogPost(blogPostDTO);
            assertEquals(blogPostDTO.getContent(),blogPostResponse.getContent());
            assertEquals(blogPostDTO.getTitle(),blogPostResponse.getTitle());
            assertEquals(blogPost.getAuthor().getUsername(),blogPostResponse.getAuthor().getUsername());



        }
    }

    @Test
    void testUpdateBlogPost_Failure(){


        BlogPostDTO blogPostDTO = new BlogPostDTO();
        blogPostDTO.setTitle("new-fake-title");
        blogPostDTO.setContent("new-fake-content");
        blogPostDTO.setId(1L);
        Set<CategoryDTO> dtos = new HashSet<>();
        for(Category c : blogPost.getCategories()){
            dtos.add(new CategoryDTO(c.getName()));
        }
        blogPostDTO.setCategories( dtos );

        try(MockedStatic<SecurityUtils>  utilities = mockStatic(SecurityUtils.class)){
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            //blog post authors
            utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);

            when(blogPostDao.findById(blogPostDTO.getId())).thenReturn(Optional.of(blogPost));
            //logged in user
            utilities .when(() -> SecurityUtils.isUser(user1)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user1)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user1)).thenReturn(false);
            when(userService.getbyId(2L)).thenReturn( user1);
            when(userDao.findById(2L)).thenReturn(Optional.of(user1));
            mockStatic(BlogPostService.class).
                    when(()->BlogPostService.canUpdateOrDelete(user1,user)).thenReturn(false);



            assertThrows(DoNotHavePermissionError.class,()->blogPostService.createOrUpdateBlogPost(blogPostDTO));



        }
    }

    @Test
    void testDeleteBlogPost_Success(){
        try(MockedStatic<SecurityUtils>  utilities = mockStatic(SecurityUtils.class)){

            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);

            when(userDao.findById(1L)).thenReturn( Optional.of(user));
            when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
            mockStatic(BlogPostService.class).
                    when(()->BlogPostService.canUpdateOrDelete(user,user)).thenReturn(true);

         BlogPostResponse blogPostResponse=   blogPostService.deleteBlogPost(1L);
         assertEquals("fake-title",blogPostResponse.getTitle());
         assertEquals("fake-content",blogPostResponse.getContent());



        }
    }

    @Test
    void testDeleteBlogPost_Failure(){
        try(MockedStatic<SecurityUtils>  utilities = mockStatic(SecurityUtils.class)){

            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            utilities .when(() -> SecurityUtils.isUser(user)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user)).thenReturn(false);


            //logged in user
            when(userDao.findById(2L)).thenReturn(Optional.of(user1));
            utilities .when(() -> SecurityUtils.isUser(user1)).thenReturn(false);
            utilities.when(()->SecurityUtils.isEditor(user1)).thenReturn(true);
            utilities.when(()->SecurityUtils.isAdmin(user1)).thenReturn(false);


            when(blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
            mockStatic(BlogPostService.class).
                    when(()->BlogPostService.canUpdateOrDelete(user1,user)).thenReturn(false);


            assertThrows(DoNotHavePermissionError.class,()->blogPostService.deleteBlogPost(1L));



        }
    }


    @Test
    void testGetBlogsByTitleAndUserId_Success(){

        List<BlogPost> blogPosts = new ArrayList<>();
        blogPosts.add(blogPost);
        when(blogPostDao.findByTitleAndAuthor("fake-title",1L)).thenReturn(blogPosts);

      List<BlogPostResponse> blogPostResponses
              = blogPostService.getBlogsByTitleAndUserId("fake-title",1L);

      assertEquals(blogPosts.size(),blogPostResponses.size());
      assertEquals(blogPosts.get(0).getTitle(),blogPostResponses.get(0).getTitle());
      assertEquals(blogPosts.get(0).getContent(),blogPostResponses.get(0).getContent());

    }

}
