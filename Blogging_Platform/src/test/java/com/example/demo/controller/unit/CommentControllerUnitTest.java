package com.example.demo.controller.unit;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.demo.config.JwtAuthFilter;
import com.example.demo.controller.CommentController;
import com.example.demo.dto.CommentDTO;
import com.example.demo.dto.CommentReact;
import com.example.demo.enums.Reaction;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.InvalidReactException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.CommentResponse;
import com.example.demo.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CommentController.class)
public class CommentControllerUnitTest {

	@MockBean
	CommentService commentService;

	@MockBean
	JwtAuthFilter authFilter;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	MockMvc mockMvc;

	BlogPost blogPost;
	User user;
	Comment comment;

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
		set.add("fake-role");
		user.setRoles(set);

		blogPost = new BlogPost();
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

		comment = new Comment();
		comment.setBlogPost(blogPost);
		comment.setContent("fake-comment");
		comment.setFunnyCount(0L);
		comment.setId(1L);
		comment.setLikeCount(0L);
		comment.setLoveCount(0L);
		comment.setReactedUsers(new HashSet<>());
		comment.setUser(user);
		blogPost.setComments(new ArrayList<>(List.of(comment)));

	}

	@Test
	public void testAddComments() throws Exception {
		CommentDTO commentDto = new CommentDTO();
		commentDto.setMessage("fake-comment");

		BlogPostResponse blogPostResponse = BlogPostResponse.convertBlogPostRespons(blogPost);

		when(commentService.createOrUpdateComment(any(CommentDTO.class), eq(1L))).thenReturn(blogPostResponse);

		mockMvc.perform(MockMvcRequestBuilders.post("/comment").param("blogPostId", "1")
				.content(objectMapper.writeValueAsString(commentDto)).contentType(MediaType.APPLICATION_JSON))

				.andExpect(status().isCreated()).andDo(print())
				.andExpect(jsonPath("$.comments[0].content").value("fake-comment"));
		verify(commentService).createOrUpdateComment(any(CommentDTO.class), eq(1L));
	}

	@Test
	public void testAddComments_WithInvalidId_ResourceNotFoundException() throws Exception {
		CommentDTO commentDto = new CommentDTO();
		commentDto.setMessage("fake-comment");

		when(commentService.createOrUpdateComment(any(CommentDTO.class), eq(1L)))
				.thenThrow(new ResourceNotFoundException("no blog post found with this id"));

		mockMvc.perform(MockMvcRequestBuilders.post("/comment").param("blogPostId", "1")
				.content(objectMapper.writeValueAsString(commentDto)).contentType(MediaType.APPLICATION_JSON))

				.andExpect(status().isNotFound()).andDo(print())
				.andExpect(jsonPath("$.msg").value("no blog post found with this id"));

		verify(commentService).createOrUpdateComment(any(CommentDTO.class), eq(1L));
	}

	@Test
	public void testUpdateComments() throws Exception {
		CommentDTO commentDto = new CommentDTO();
		commentDto.setMessage("fake-comment");
		commentDto.setCommentId(1L);

		BlogPostResponse blogPostResponse = BlogPostResponse.convertBlogPostRespons(blogPost);

		when(commentService.createOrUpdateComment(any(CommentDTO.class), eq(1L))).thenReturn(blogPostResponse);

		mockMvc.perform(MockMvcRequestBuilders.put("/comment").param("blogPostId", "1")
				.content(objectMapper.writeValueAsString(commentDto)).contentType(MediaType.APPLICATION_JSON))

				.andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$.comments[0].content").value("fake-comment"));
		verify(commentService).createOrUpdateComment(any(CommentDTO.class), eq(1L));
	}

	@Test
	public void testUpdateComments_WithInvalidId_ResourceNotFoundException() throws Exception {
		CommentDTO commentDto = new CommentDTO();
		commentDto.setMessage("fake-comment");
		commentDto.setCommentId(1L);

		BlogPostResponse blogPostResponse = BlogPostResponse.convertBlogPostRespons(blogPost);

		when(commentService.createOrUpdateComment(any(CommentDTO.class), eq(1L)))
				.thenThrow(new ResourceNotFoundException("No comment is present with the provided id..."));

		mockMvc.perform(MockMvcRequestBuilders.post("/comment").param("blogPostId", "1")
				.content(objectMapper.writeValueAsString(commentDto)).contentType(MediaType.APPLICATION_JSON))

				.andExpect(status().isNotFound()).andDo(print())
				.andExpect(jsonPath("$.msg").value("No comment is present with the provided id..."));

		verify(commentService).createOrUpdateComment(any(CommentDTO.class), eq(1L));
	}

	@Test
	public void testUpdateComments_WithWrongUser_DoNotHavePermissionError() throws Exception {
		CommentDTO commentDto = new CommentDTO();
		commentDto.setMessage("fake-comment");
		commentDto.setCommentId(1L);

		when(commentService.createOrUpdateComment(any(CommentDTO.class), eq(1L)))

				.thenThrow(new DoNotHavePermissionError("You can not make changes on this comment!"));

		mockMvc.perform(MockMvcRequestBuilders.post("/comment").param("blogPostId", "1")
				.content(objectMapper.writeValueAsString(commentDto)).contentType(MediaType.APPLICATION_JSON))

				.andExpect(status().isForbidden()).andDo(print())
				.andExpect(jsonPath("$.msg").value("You can not make changes on this comment!"));

		verify(commentService).createOrUpdateComment(any(CommentDTO.class), eq(1L));
	}

	@Test
	public void testGetById() throws Exception {
		CommentResponse commentResponse = CommentResponse.convertCommentResponse(comment);
		when(commentService.getById(1L)).thenReturn(commentResponse);

		mockMvc.perform(MockMvcRequestBuilders.get("/comment/1")).andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$.id").value(1));

		verify(commentService).getById(1L);

	}

	@Test
	public void testGetById_WithInvalidId_FailureWithResourceNotFoundException() throws Exception {

		when(commentService.getById(1L)).thenThrow(new ResourceNotFoundException("Resource is not present!"));

		mockMvc.perform(MockMvcRequestBuilders.get("/comment/1")).andExpect(status().isNotFound()).andDo(print())
				.andExpect(jsonPath("$.msg").value("Resource is not present!"));

		verify(commentService).getById(1L);

	}

	@Test
	public void testCommentReact() throws Exception {
		CommentReact commmentReact = new CommentReact();
		commmentReact.setId(1L);
		commmentReact.setReaction(Reaction.LIKE);

		comment.setLikeCount(comment.getLikeCount() + 1);
		comment.setReactedUsers(new HashSet<>(Set.of(user)));

		CommentResponse commentResponse = CommentResponse.convertCommentResponse(comment);

		when(commentService.reactComment(any(CommentReact.class))).thenReturn(commentResponse);

		mockMvc.perform(MockMvcRequestBuilders.post("/comment/react")
				.content(objectMapper.writeValueAsString(commmentReact)).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.likeCount").value(1));
		verify(commentService).reactComment(any(CommentReact.class));
	}

	@Test
	public void testCommentReact_MultipleReacts_InvalidReactException() throws Exception {
		CommentReact commmentReact = new CommentReact();
		commmentReact.setId(1L);
		commmentReact.setReaction(Reaction.LIKE);

		comment.setLikeCount(comment.getLikeCount() + 1);
		comment.setReactedUsers(new HashSet<>(Set.of(user)));

		CommentResponse commentResponse = CommentResponse.convertCommentResponse(comment);

		when(commentService.reactComment(any(CommentReact.class)))
				.thenThrow(new InvalidReactException("You have already reacted!"));

		mockMvc.perform(MockMvcRequestBuilders.post("/comment/react")
				.content(objectMapper.writeValueAsString(commmentReact)).contentType(MediaType.APPLICATION_JSON))

				.andDo(print()).andExpect(status().isBadRequest()).andDo(print())
				.andExpect(jsonPath("$.msg").value("You have already reacted!"));

		verify(commentService).reactComment(any(CommentReact.class));
	}

	@Test
	public void testDeleteComment() throws Exception {
		CommentResponse commentResponse = CommentResponse.convertCommentResponse(comment);
		when(commentService.deleteComment(1L, 1L)).thenReturn(commentResponse);
		mockMvc.perform(MockMvcRequestBuilders.delete("/comment/1/blogpost/1")).andExpect(status().isOk())
				.andDo(print()).andExpect(jsonPath("$.id").value(1));

		verify(commentService).deleteComment(1L, 1L);

	}

	@Test
	public void testDeleteComment_WithInvalid_FailureWithResourceNotFoundException() throws Exception {
		CommentResponse commentResponse = CommentResponse.convertCommentResponse(comment);
		when(commentService.deleteComment(1L, 1L))
				.thenThrow(new ResourceNotFoundException("Resource is not present..."));
		mockMvc.perform(MockMvcRequestBuilders.delete("/comment/1/blogpost/1")).andExpect(status().isNotFound())
				.andDo(print()).andExpect(jsonPath("$.msg").value("Resource is not present..."));

		verify(commentService).deleteComment(1L, 1L);

	}

	@Test
	public void testDeleteComment_WithInvalid_FailureWithDoNotHavePermissionError() throws Exception {

		when(commentService.deleteComment(1L, 1L))
				.thenThrow(new DoNotHavePermissionError("You are not allowed to change other's comment!"));
		mockMvc.perform(MockMvcRequestBuilders.delete("/comment/1/blogpost/1")).andExpect(status().isForbidden())
				.andDo(print()).andExpect(jsonPath("$.msg").value("You are not allowed to change other's comment!"));

		verify(commentService).deleteComment(1L, 1L);

	}
}
