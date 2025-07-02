package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.UserResponse;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Mock
    private UserService userService;


    @InjectMocks
    private UserController  userController;
    User user;
    private ObjectMapper objectMapper  = new ObjectMapper();
    private MockMvc   mockMvc;
    @BeforeEach
    void setUp(){
        mockMvc= MockMvcBuilders.standaloneSetup(userController).build();


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
        BlogPost blogPost = new BlogPost();
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
    void testCreateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("fake-username");
        userDTO.setEmail("fake@ppp.com");
        userDTO.setBio("fake-bio");
        userDTO.setPassword("fake-password");
        Set<String> set = new HashSet<>();
        set.add("fake-role");
        userDTO.setRoles(set);
//
//        User user = new User();
       user.setUsername(userDTO.getUsername());
       user.setBio(userDTO.getBio());
        user.setEmail(userDTO.getEmail());
//        user.setId(1L);
//        user.setFollowing(0L);
//        user.setTotalPosts(0L);
//        user.setFollowing(0L);
//        user.setFollowers(0L);
       user.setPassword(userDTO.getPassword());
        user.setRoles(userDTO.getRoles());

        UserResponse userResponse = UserResponse.convertUserResponse(user);

        when(userService.createUser(any(UserDTO.class))).thenReturn(user);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO))
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("fake-username"))
                .andExpect(jsonPath("$.email").value("fake@ppp.com"));
        verify(userService).createUser(any(UserDTO.class));


    }

    @Test
    void testGetUserId() throws Exception {

//        User user = new User();
//        user.setUsername("fake-username");
//        user.setBio("fake-bio");
//        user.setEmail("fake@ppp.com");
//        user.setId(1L);
//        user.setFollowing(0L);
//        user.setTotalPosts(0L);
//        user.setFollowing(0L);
//        user.setFollowers(0L);
//        user.setPassword("fake-password");
//        Set<String> set = new HashSet<>();
//        set.add("fake-role");
//        user.setRoles(set);

        when(userService.getbyId(1L)).thenReturn(user);
        mockMvc.perform(get("/users/1")

                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("fake-username"))
                .andExpect(jsonPath("$.email").value("fake@ppp.com"));
        verify(userService).getbyId(1L);

    }


    @Test
    void testGetPostsByUserId() throws Exception {


        when(userService.getbyId(1L)).thenReturn(user);
//      String s=  mockMvc.perform(get("/users/1/posts")).andReturn().getResponse().getContentAsString();
//      System.out.println(s);
        mockMvc.perform(get("/users/1/posts")

                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("fake-username"))
                .andExpect(jsonPath("$.email").value("fake@ppp.com"))
                .andExpect(jsonPath("$.blogPosts[0].content").value("blog content"));

        verify(userService).getbyId(1L);

    }
    @Test
    void testGetAll() throws Exception {
//        User user = new User();
//        user.setUsername("fake-username");
//        user.setBio("fake-bio");
//        user.setEmail("fake@ppp.com");
//        user.setId(1L);
//        user.setFollowing(0L);
//        user.setTotalPosts(0L);
//        user.setFollowing(0L);
//        user.setFollowers(0L);
        UserResponse uR = UserResponse.convertUserResponse(user);
       when( userService.getAll()).thenReturn(List.of(uR));

        mockMvc.perform(get("/users")

                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(1L))
                .andExpect(jsonPath("$.[0].username").value("fake-username"))
                .andExpect(jsonPath("$.[0].email").value("fake@ppp.com"));
        verify(userService).getAll();
    }

    @Test
    void testUpdate() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("fake-new-username");
        userDTO.setEmail("fake@ppp.com");
        userDTO.setBio("fake-new-bio");
        userDTO.setPassword("fake-new-password");
        Set<String> set = new HashSet<>();
        set.add("fake-role");
        userDTO.setRoles(set);

        user.setUsername(userDTO.getUsername());
        user.setBio(userDTO.getBio());
        user.setEmail(userDTO.getEmail());

        user.setPassword(userDTO.getPassword());
        user.setRoles(userDTO.getRoles());



        UserResponse userResponse = UserResponse.convertUserResponse(user);

        when(userService.updateUser(any(UserDTO.class))).thenReturn(user);

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("fake-new-username"))
                .andExpect(jsonPath("$.bio").value("fake-new-bio"))
                .andExpect(jsonPath("$.email").value("fake@ppp.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(userService).updateUser(any(UserDTO.class));

    }
    @Test
    void testDelete() throws Exception {

        UserResponse uR = UserResponse.convertUserResponse(user);

   when(userService.deleteUser(1L)).thenReturn(uR);
        mockMvc.perform(delete("/users/1")
                
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("fake-username"))
                .andExpect(jsonPath("$.bio").value("fake-bio"))
                .andExpect(jsonPath("$.email").value("fake@ppp.com"));
        verify(userService).deleteUser(1L);



    }

}
