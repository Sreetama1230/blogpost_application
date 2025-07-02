package com.example.demo.response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.Comment;
import com.example.demo.model.User;

import jakarta.persistence.Column;


public class UserResponse {

	private long id;
	private String username;

	private Long followers;
	private Long following;
	private String bio;	
	private Long totalPosts;
	private Set<String> roles;
	private String email;
	private List<BlogPostResponse> blogPosts= new ArrayList<>(); 

	private List<CommentResponse> comments= new ArrayList<>(); 


	public Long getFollowers() {
		return followers;
	}



	public void setFollowers(Long followers) {
		this.followers = followers;
	}



	public Set<String> getRoles() {
		return roles;
	}



	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}



	public Long getFollowing() {
		return following;
	}



	public void setFollowing(Long following) {
		this.following = following;
	}




	public Long getTotalPosts() {
		return totalPosts;
	}



	public void setTotalPosts(Long totalPosts) {
		this.totalPosts = totalPosts;
	}





	public UserResponse(long id, String username, Long followers, Long following, String bio, Long totalPosts,
			String email, List<BlogPostResponse> blogPosts, List<CommentResponse> comments) {
		super();
		this.id = id;
		this.username = username;
		this.followers = followers;
		this.following = following;
		this.bio = bio;
		this.totalPosts = totalPosts;
		this.email = email;
		this.blogPosts = blogPosts;
		this.comments = comments;
	}



	public String getBio() {
		return bio;
	}



	public void setBio(String bio) {
		this.bio = bio;
	}



	public long getId() {
		return id;
	}



	public void setId(long id) {
		this.id = id;
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

	public UserResponse() {
		super();

	}



	public UserResponse(long id, String username, String email) {
		super();
		this.id = id;
		this.username = username;
		this.email = email;
	}






public static UserResponse convertUserResponse(User u) {
	List<BlogPost> dbBlogPosts=   u.getBlogPosts();
	List<BlogPostResponse> blogPostResponses = new ArrayList<>();
	for(BlogPost b : dbBlogPosts) {
		blogPostResponses.add(BlogPostResponse.convertBlogPostRespons(b));
	}
	List<Comment> dbComments = u.getComments();
	List<CommentResponse> commentResponses = new ArrayList<>();
	
	for(Comment c : dbComments  ) {
		commentResponses.add(CommentResponse.convertCommentResponse(c));
	}
	  UserResponse resp= 
			  new UserResponse(u.getId(),
					  u.getUsername(),u.getFollowers(),
					  u.getFollowing(),u.getBio(),
					  u.getTotalPosts(),u.getEmail(),blogPostResponses,commentResponses);
	  resp.setFollowers(u.getFollowers());
	  resp.setFollowing(u.getFollowing());
	  resp.setTotalPosts((long)u.getBlogPosts().size());
	  resp.setRoles(u.getRoles());
	  resp.setId(u.getId());
	  return resp;
	  
}


	
}
