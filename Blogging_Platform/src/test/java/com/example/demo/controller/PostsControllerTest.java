package com.example.demo.controller;

import com.example.demo.dto.ReactDTO;
import com.example.demo.exception.CustomGraphQLExceptionHandler;
import com.example.demo.exception.FollowUnFollowException;
import com.example.demo.gqlcontroller.PostsController;
import com.example.demo.gqlservice.PostsService;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.PinnedBlogPost;
import com.example.demo.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static graphql.introspection.IntrospectionQueryBuilder.build;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@GraphQlTest(PostsController.class)
@Import(CustomGraphQLExceptionHandler.class)
public class PostsControllerTest {


    @MockBean
    private PostsService postsService;
    @Autowired
    private GraphQlTester graphQlTester;




        @Test
        @WithMockUser(username = "1")
    void testSearchPosts(){
        BlogPostResponse blogPostResponse = new BlogPostResponse();
        blogPostResponse.setId(1L);
        blogPostResponse.setTitle("Fake Title");
        blogPostResponse.setContent("Fake Content");

        List<BlogPostResponse> list = List.of(blogPostResponse);
        when(postsService.searchPosts("fake")).thenReturn(list);
        graphQlTester.document("""
                        query Search{
                            searchPosts(keyword:"fake"){
                                title,
                                content
                        
                            }
                        }
                """)
                .execute()
                .path("searchPosts")
                .entityList(BlogPostResponse.class)
                .hasSize(1)
                .matches(l ->
                        l.get(0).getTitle().equals("Fake Title") &&
                                l.get(0).getContent().equals("Fake Content")
                );

        ;
    }

    @Test
    @WithMockUser(username = "1")
void testGetPost(){
        BlogPostResponse response1 = new BlogPostResponse();
        response1.setId(1L);
        response1.setTitle("Title 1");
        response1.setContent("Content 1");

        BlogPostResponse response2 = new BlogPostResponse();
        response2.setId(2L);
        response2.setTitle("Title 2");
        response2.setContent("Content 2");
List<BlogPostResponse> blogPostResponses = List.of(response1,response2);
        Page<BlogPostResponse> page = new PageImpl<>(blogPostResponses);
        when(postsService.getPosts(0,2)).thenReturn(page);
        graphQlTester.document("""
                
                query Pagination{
                   getPosts(page: 0,size:2){
                    content
                   }
                }
                
                """).execute()
                .path("getPosts")
                .entityList(BlogPostResponse.class)
                .hasSize(2)
                .matches(l-> l.get(0).getContent().equals("Content 1"));

}

@Test
@WithMockUser(username = "1")
void testGetPinnedPostsOfTheUser(){
    BlogPostResponse response1 = new BlogPostResponse();
    response1.setId(1L);
    response1.setTitle("Title 1");
    response1.setContent("Content 1");
    PinnedBlogPost pinned1 = new PinnedBlogPost( LocalDateTime.now(),response1);
    BlogPostResponse response2 = new BlogPostResponse();
    response2.setId(2L);
    response2.setTitle("Title 2");
    response2.setContent("Content 2");
    PinnedBlogPost pinned2 = new PinnedBlogPost(LocalDateTime.now(),response2);

    List<PinnedBlogPost> pinnedBlogPosts
            = List.of(pinned1,pinned2);
    when(postsService.getPinnedPostsOfTheUser(1L)).thenReturn(pinnedBlogPosts);

    graphQlTester.document("""
                     query GetPinnedPostsOfTheUser {
                      getPinnedPostsOfTheUser(uId:1){
                       pinnedDate,
                       blogPostResponse {
                        content
                       }
                      }
                    }
                    
            """).execute().path("getPinnedPostsOfTheUser")
            .entityList
                    (PinnedBlogPost.class)
            .hasSize(2)
            .matches(l->l.get(0)
                    .getBlogPostResponse()
                    .getContent()
                    .equals("Content 1"));



}

@Test
@WithMockUser(username = "1")
void testGetFollowers(){
    UserResponse  userResponse = new UserResponse();
    userResponse.setId(1L);
    userResponse.setTotalPosts(1L);
    userResponse.setEmail("fake@gmail.com");
    userResponse.setFollowers(0L);
    userResponse.setFollowing(1L);
    userResponse.setBio("fake-bio");
    userResponse.setUsername("fake_username");
    userResponse.setRoles(new HashSet<>(Set.of("ROLE_FAKE")));
    userResponse.setBlogPosts(new ArrayList<>());

    when(postsService.getFollowers(2L)).thenReturn(List.of(userResponse));

    graphQlTester.document("""
            query GetFollowers{
                getFollowers(uId:2){
                        username
                }
            }
            
          
            """).execute().path("getFollowers")
            .entityList(UserResponse.class).hasSize(1)
            .matches(l->l.get(0).getUsername().equals("fake_username"));
}

    @Test
    @WithMockUser(username = "1")
    void testGetFollowerings(){
        UserResponse  userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setTotalPosts(1L);
        userResponse.setEmail("fake@gmail.com");
        userResponse.setFollowers(1L);
        userResponse.setFollowing(0L);
        userResponse.setBio("fake-bio");
        userResponse.setUsername("fake_username");
        userResponse.setRoles(new HashSet<>(Set.of("ROLE_FAKE")));
        userResponse.setBlogPosts(new ArrayList<>());

        when(postsService.getFollowings(2L)).thenReturn(List.of(userResponse));

        graphQlTester.document("""
                        query GetFollowings{
                            getFollowings(uId:2){
                                    username
                            }
                        }
          
            """).execute().path("getFollowings")
                .entityList(UserResponse.class).hasSize(1)
                .matches(l->l.get(0).getUsername().equals("fake_username"));
    }

    @Test
    @WithMockUser(username = "1")
    void testTrendingPosts(){
        BlogPostResponse response1 = new BlogPostResponse();
        response1.setId(1L);
        response1.setTitle("Title 1");
        response1.setContent("Content 1");

        BlogPostResponse response2 = new BlogPostResponse();
        response2.setId(2L);
        response2.setTitle("Title 2");
        response2.setContent("Content 2");
        List<BlogPostResponse> blogPostResponses = List.of(response1,response2);

        Page<BlogPostResponse> page = new PageImpl<>(blogPostResponses);
        when(postsService.trendingPosts()).thenReturn(page);
        graphQlTester.document("""
                        query TrendingPosts{
                            trendingPosts{
                                   content,
                                   createAt
                            }
                        }
                """).execute()
                .path("trendingPosts")
                .entityList(BlogPostResponse.class)
                .hasSize(2)
                .matches(l->l.get(0).getContent().equals("Content 1"));

    }

    @Test
    @WithMockUser(username = "1")
    void testUserLikedPost(){
        BlogPostResponse response1 = new BlogPostResponse();
        response1.setId(1L);
        response1.setTitle("Title 1");
        response1.setContent("Content 1");

        BlogPostResponse response2 = new BlogPostResponse();
        response2.setId(2L);
        response2.setTitle("Title 2");
        response2.setContent("Content 2");
        List<BlogPostResponse> blogPostResponses = List.of(response1,response2);

      when(postsService.userLikedPost(1L)).thenReturn(blogPostResponses);

      graphQlTester.document("""
                      query   UserLikedPost{
                         userLikedPost(uId:1){
                                 content,
                                 createAt
                          }
                      }
              """).execute().path("userLikedPost")
              .entityList(BlogPostResponse.class)
              .hasSize(2)
              .matches(l->l.get(1).getContent().equals("Content 2"));

    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN", "EDITOR"})
    void testBlockedUsers(){
        UserResponse  userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setTotalPosts(1L);
        userResponse.setEmail("fake@gmail.com");
        userResponse.setBio("fake-bio");
        userResponse.setUsername("fake_username");
        userResponse.setRoles(new HashSet<>(Set.of("ROLE_FAKE")));


        when(postsService.getBlockedUsers(2L)).thenReturn(List.of(userResponse));

        graphQlTester.document("""
                query BlockedUsers{
                  blockedUsers (uId:2){
                    id
                    username
                    }
                }
                """)
                .execute()
                .path("blockedUsers")
                .entityList(UserResponse.class)
                .hasSize(1)
                .matches(l->l.get(0)
                        .getUsername().equals("fake_username"));

    }

    // mutation
    @Test
    @WithMockUser(username = "1", roles = {"ADMIN", "EDITOR","USER"})
void  testSetReaction(){
        ReactDTO reactDTO = new ReactDTO(1L,true,1L);

        BlogPostResponse response1 = new BlogPostResponse();
        response1.setId(1L);
        response1.setTitle("Title 1");
        response1.setContent("Content 1");

        when(postsService.setReaction(any(ReactDTO.class))).thenReturn(response1);

        graphQlTester.document("""
                
                mutation SetReaction {
                  setReaction(
                     request : {
                      bpId: 1,
                      uId: 1,
                      reaction: true
                    }
                  ) {
                    content
                    createAt
                  }
                }
                
                
                
                """).execute().path("setReaction").entity(BlogPostResponse.class).get().getContent().equals("Content 1");


  }

  @Test
  @WithMockUser(username = "1", roles = {"ADMIN", "EDITOR","USER"})
  void testPinnedPost(){

      BlogPostResponse response1 = new BlogPostResponse();
      response1.setId(1L);
      response1.setTitle("Title 1");
      response1.setContent("Content 1");
      PinnedBlogPost pinned1 = new PinnedBlogPost( LocalDateTime.now(),response1);

      when(postsService.pinnedPost(1L,1L)).thenReturn(pinned1);

      graphQlTester.document("""
              
              
              mutation PinAPost {
               pinnedPost(uId:1 , bpId: 1 ){
                 pinnedDate
                  blogPostResponse {
                    content
                   }
                }
              }
              
              
              
              """).execute().path("pinnedPost").entity(PinnedBlogPost.class).get().getBlogPostResponse().getContent().equals("Content 1");

  }

  @Test
  @WithMockUser(username = "1", roles = {"ADMIN", "EDITOR","USER"})
  void testFollowOrUnFollowAuthor(){
      UserResponse  userResponse = new UserResponse();
      userResponse.setId(1L);
      userResponse.setTotalPosts(1L);
      userResponse.setEmail("fake@gmail.com");
      userResponse.setBio("fake-bio");
      userResponse.setUsername("fake_username");
      userResponse.setRoles(new HashSet<>(Set.of("ROLE_FAKE")));


      UserResponse  userResponse1 = new UserResponse();
      userResponse1.setId(2L);
      userResponse1.setTotalPosts(1L);
      userResponse1.setEmail("fake1@gmail.com");
      userResponse1.setBio("fake1-bio");
      userResponse1.setUsername("fake1_username");
      userResponse1.setRoles(new HashSet<>(Set.of("ROLE_FAKE1")));


      when(postsService.followOrUnFollowAuthor(1L,2L)).thenReturn(List.of(userResponse,userResponse1));

      graphQlTester.document("""
              mutation FollowOrUnFollowAuthor{
                  followOrUnFollowAuthor(follower: 1, followee:2){
                      username
                  }
              }
              """).execute().path("followOrUnFollowAuthor").entityList(UserResponse.class).hasSize(2).matches(l->l.get(0).getUsername().equals(userResponse.getUsername()));




  }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN", "EDITOR","USER"})
    void testFollowOrUnFollowAuthor_Failure(){
        UserResponse  userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setTotalPosts(1L);
        userResponse.setEmail("fake@gmail.com");
        userResponse.setBio("fake-bio");
        userResponse.setUsername("fake_username");
        userResponse.setRoles(new HashSet<>(Set.of("ROLE_FAKE")));

        when(postsService.followOrUnFollowAuthor(1L,1L)).thenThrow(new FollowUnFollowException("You can not follow yourself!"));

        graphQlTester.document("""
              mutation FollowOrUnFollowAuthor{
                  followOrUnFollowAuthor(follower: 1, followee:1){
                      username
                  }
              }
              """).execute().errors()    .satisfy(errors -> {
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).getMessage().equals("You can not follow yourself!"));

        });

      verify(postsService).followOrUnFollowAuthor(1L, 1L);

    }

  @Test
  @WithMockUser(username = "1", roles = {"ADMIN", "EDITOR"})
  void testBlockUser(){
      UserResponse  userResponse = new UserResponse();
      userResponse.setId(1L);
      userResponse.setTotalPosts(1L);
      userResponse.setEmail("fake@gmail.com");
      userResponse.setBio("fake-bio");
      userResponse.setUsername("fake_username");
      userResponse.setRoles(new HashSet<>(Set.of("ROLE_FAKE")));


      UserResponse  userResponse1 = new UserResponse();
      userResponse1.setId(2L);
      userResponse1.setTotalPosts(1L);
      userResponse1.setEmail("fake1@gmail.com");
      userResponse1.setBio("fake1-bio");
      userResponse1.setUsername("fake1_username");
      userResponse1.setRoles(new HashSet<>(Set.of("ROLE_FAKE1")));


    when(postsService.blockUser(1L,2L)).thenReturn(List.of(userResponse,userResponse1));

    graphQlTester.document("""
                    mutation BlockUser{
                       blockUser (blockerId: 1, blockedUserId:2){
                            username
                        }
                    }
            """).execute().path("blockUser").entityList(UserResponse.class).hasSize(2)
            .matches(l->l.get(0).getUsername().equals(userResponse.getUsername()));
  }



}
