package com.example.demo.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
import com.example.demo.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

public class BlogPostDetailsResponse {
	private String username;
    private Long id;
	private String email;

	private List<BlogPostResponse> blogPosts= new ArrayList<>(); ;

	private List<CommentResponse> comments= new ArrayList<>(); ;


	public BlogPostDetailsResponse() {
		super();

	}


	public BlogPostDetailsResponse(String username, String email, List<BlogPostResponse> blogPosts,
			List<CommentResponse> comments) {
		super();
		this.username = username;
		this.email = email;
		this.blogPosts = blogPosts;
		this.comments = comments;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public List<BlogPostResponse> getBlogPosts() {
		return blogPosts;
	}


	public void setBlogPosts(List<BlogPostResponse> blogPosts) {
		this.blogPosts = blogPosts;
	}


	public List<CommentResponse> getComments() {
		return comments;
	}


	public void setComments(List<CommentResponse> comments) {
		this.comments = comments;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public static BlogPostDetailsResponse convertBlogPostDetailsResponse(User b) {
		List<BlogPost> dbblogs = b.getBlogPosts();
		List<Comment> dbcom = b.getComments();
		List<CommentResponse> comments = new ArrayList<>();
		List<BlogPostResponse> blogs = new ArrayList<>();
		for(BlogPost c : dbblogs) {
			 blogs.add(BlogPostResponse.convertBlogPostRespons(c));
		}
		for(Comment c : dbcom) {
		  comments.add(CommentResponse.convertCommentResponse(c));
		}
		BlogPostDetailsResponse blogPostDetailsResponse=
				new BlogPostDetailsResponse(b.getUsername(),b.getEmail(),  blogs, comments);
		blogPostDetailsResponse.setId(b.getId());
		return blogPostDetailsResponse;
	}
}
