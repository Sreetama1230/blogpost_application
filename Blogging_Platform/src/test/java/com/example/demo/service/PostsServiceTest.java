package com.example.demo.service;


import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.ReactDTO;
import com.example.demo.gqlservice.PostsService;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.PinnedBlogPost;
import com.example.demo.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class PostsServiceTest {

    @Mock
    private BlogPostDao blogPostDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    PostsService postsService;


    private User user;
    private User user1;
    private BlogPost blogPost;
    private Category category;
    PageRequest pageable;

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
        blogPost.setLikes(0L);

        pageable = PageRequest.of(0,10);
        HashSet<BlogPost> set = new HashSet<>();
        set.add(blogPost);
        category.setBlogPosts(set);
        user.setBlogPosts(new ArrayList<>(List.of(blogPost)));


    }

    @Test
    void testSearchPosts_Success()  {

        String keyword="fake";
        when(blogPostDao.searchPosts(keyword)).thenReturn(List.of(blogPost));

            List<BlogPostResponse> blogPostResponses =postsService.searchPosts(keyword);
            assertEquals(List.of(blogPost).size(),blogPostResponses.size());
           assertEquals(blogPost.getContent(),blogPostResponses.get(0).getContent());



    }
    @Test
    void testGetPosts(){

        Page<BlogPost> page = new PageImpl<>(List.of(blogPost), pageable, List.of(blogPost).size());

        when(blogPostDao.findAll(pageable)).thenReturn(page);
       Page<BlogPostResponse> blogPostResponses=  postsService.getPosts(1,10);

       assertEquals(page.getTotalPages(),blogPostResponses.getTotalPages());
       assertEquals(page.getTotalElements(),blogPostResponses.getTotalElements());



    }
    @Test
    void testGetPinnedPostsOfTheUser(){
        blogPost.setPinnedBy(user);
        user.setPinnedBlogPosts(List.of(blogPost));

        //logged in user
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
       List<PinnedBlogPost> blogPosts= postsService.getPinnedPostsOfTheUser(1L);

       assertEquals(user.getPinnedBlogPosts().size(),blogPosts.size());
       assertEquals(user.getPinnedBlogPosts().get(0).getContent(),blogPosts.get(0).getBlogPostResponse().getContent());
    }
    @Test
    void testGetFollowers(){
        Set<User> followersUser = new HashSet<>();
        followersUser.add(user1);
        user.setListfollowers(followersUser);
        user.setFollowers(1L);
        // user1 is follower of user

        Set<User> followingUser = new HashSet<>();
        followingUser.add(user);
        user1.setListfollowing(followingUser);
        user1.setFollowing(1L);

        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        List<UserResponse> followers = postsService.getFollowers(1L);
        assertEquals(1,followers.size());
        assertEquals( "fake-username-2",followers.get(0).getUsername());

    }
    @Test
    void testGetFollowerings(){

        Set<User> followersUser = new HashSet<>();
        followersUser.add(user1);
        user.setListfollowers(followersUser);
        user.setFollowers(1L);
        // user1 is follower of user

        Set<User> followingUser = new HashSet<>();
        followingUser.add(user);
        user1.setListfollowing(followingUser);
        user1.setFollowing(1L);

        when(userDao.findById(2L)).thenReturn(Optional.of(user1));

        List<UserResponse> followerings = postsService.getFollowings(2L);
        assertEquals(1,followerings.size());
        assertEquals( "fake-username",followerings.get(0).getUsername());

    }
    @Test
    void testTrendingPosts(){
        Page<BlogPost> page = new PageImpl<>(List.of(blogPost), pageable, List.of(blogPost).size());
        when(blogPostDao.findTopNByOrderByLikesDesc(pageable)).thenReturn(page);

        Page <BlogPostResponse> blogPostResponses =   postsService.trendingPosts();

        assertEquals(page.getTotalPages(),blogPostResponses.getTotalPages());
        assertEquals(page.getTotalElements(),blogPostResponses.getTotalElements());
    }
    @Test
  void testLikedBlogPostResponses(){
        user.setLikedBlogPosts(List.of(blogPost));
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
      List<BlogPostResponse> blogPostResponses=  postsService.userLikedPost(1L);

      assertEquals(user.getLikedBlogPosts().size(),blogPostResponses.size());
      assertEquals(user.getLikedBlogPosts().get(0).getContent(),blogPostResponses.get(0).getContent());

  }

    @Test
    void testGetBlockedUsers(){
        Set<User> users = new HashSet<>();
        users.add(user1);
        user.setBlockedUsers(users);
        when (userDao.findById(1L)).thenReturn(Optional.of(user));
        List<UserResponse> userResponses=  postsService.getBlockedUsers(1L);

        assertEquals(1,userResponses.size());
        assertEquals(user1.getUsername(),userResponses.get(0).getUsername());
    }

        //@mutation
  @Test
        void testSetReaction(){
            ReactDTO request = new ReactDTO(1L,true,1L);
            when( blogPostDao.findById(request.getBpId())).thenReturn(Optional.of(blogPost));
            when(userDao.findById(request.getuId())).thenReturn(Optional.of(user));
           long n = blogPost.getLikes();
            BlogPostResponse blogPostResponse=  postsService.setReaction(request);
            assertNotNull(   blogPostResponse);
            assertEquals(Optional.of(n + 1L).get(),blogPostResponse.getLikes());
            assertEquals(blogPost.getContent(),blogPostResponse.getContent());

        }
        @Test
        void testPinnedPost(){
            when( blogPostDao.findById(1L)).thenReturn(Optional.of(blogPost));
            when(userDao.findById(1L)).thenReturn(Optional.of(user));
            int n =user.getPinnedBlogPosts().size();
            PinnedBlogPost pinnedBlogPost=    postsService.pinnedPost(1L,1L);
            assertNotNull(pinnedBlogPost);
            assertEquals(n+1,user.getPinnedBlogPosts().size());
            assertEquals((Long)1L,pinnedBlogPost.getBlogPostResponse().getId());
            assertEquals(  blogPost.getContent(), pinnedBlogPost.getBlogPostResponse().getContent());

        }
        @Test
        void testFollowOrUnFollowAuthor(){

           when (userDao.findById(1L)).thenReturn(Optional.of(user));
           when(userDao.findById(2L)).thenReturn(Optional.of(user1));

         List<UserResponse> userResponses=  postsService.followOrUnFollowAuthor(2L,1L);
            assertEquals ( true,user.getListfollowers().contains(user1));
            assertEquals(true,user1.getListfollowing().contains(user));
         assertEquals(2,userResponses.size());
         assertEquals(user.getUsername(),userResponses.get(0).getUsername());
         assertEquals(user1.getUsername(),userResponses.get(1).getUsername());
    }
    @Test
    void testBlockUser(){
        when (userDao.findById(1L)).thenReturn(Optional.of(user));
        when(userDao.findById(2L)).thenReturn(Optional.of(user1));

       List<UserResponse> userResponses= postsService.blockUser(1L,2L);
        assertEquals(true, user.getBlockedUsers().contains(user1));
       assertEquals(2,userResponses.size());
       assertEquals(user.getUsername(),userResponses.get(0).getUsername());
       assertEquals(user1.getUsername(),userResponses.get(1).getUsername());
    }

}
