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


public class BlogPostResponse {

	private Long id;
	private String title;

	private String content;

	private BlogPostUserResponse author;

	private Set<CategoryResponse> categories= new HashSet<>();; 

	private List<CommentResponse> comments= new ArrayList<>();;

	private LocalDateTime createAt;

	private LocalDateTime updateAt;
	
	private  Long likes;
	
	private  Long dislikes;

	public BlogPostResponse() {
		super();

	}

	public BlogPostResponse(String title, String content, BlogPostUserResponse author, Set<CategoryResponse> categories
			, List<CommentResponse> comments, LocalDateTime createAt, LocalDateTime updateAt) {

	this.title=title;
	this.content=content;
	this.author=author;
		this.categories = categories;
		this.comments = comments;
		this.createAt = createAt;
		this.updateAt = updateAt;

	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}



	public BlogPostResponse( Long id ,String title, String content, BlogPostUserResponse author, Set<CategoryResponse> categories,
			List<CommentResponse> comments, LocalDateTime createAt, LocalDateTime updateAt) {
		super();

		this.title = title;
		this.id=id;
		this.content = content;
		this.author = author;
		this.categories = categories;
		this.comments = comments;
		this.createAt = createAt;
		this.updateAt = updateAt;
	}




	public Long getLikes() {
		return likes;
	}




	public void setLikes(Long likes) {
		this.likes = likes;
	}




	public Long getDislikes() {
		return dislikes;
	}




	public void setDislikes(Long dislikes) {
		this.dislikes = dislikes;
	}




	public BlogPostUserResponse getAuthor() {
		return author;
	}




	public void setAuthor(BlogPostUserResponse author) {
		this.author = author;
	}




	public Set<CategoryResponse> getCategories() {
		return categories;
	}

	public void setCategories(Set<CategoryResponse> categories) {
		this.categories = categories;
	}

	public List<CommentResponse> getComments() {
		return comments;
	}

	public void setComments(List<CommentResponse> comments) {
		this.comments = comments;
	}

	public LocalDateTime getCreateAt() {
		return createAt;
	}

	public void setCreateAt(LocalDateTime createAt) {
		this.createAt = createAt;
	}

	public LocalDateTime getUpdateAt() {
		return updateAt;
	}

	public void setUpdateAt(LocalDateTime updateAt) {
		this.updateAt = updateAt;
	}



	public BlogPostResponse(String title, String content, LocalDateTime createAt,BlogPostUserResponse author) {

		this.title=title;
		this.content=content;
		this.createAt = createAt;
		this.author=author;


	}

	public static BlogPostResponse convertBlogPostRespons(BlogPost b) {
		
		Set<Category> dBCategory = b.getCategories();
		List<Comment> dbComments = b.getComments();
		Set<CategoryResponse> categoryResponses = new HashSet<>();
		List<CommentResponse> commentResponses = new ArrayList<>();
		for(  Category  c : dBCategory) {
			categoryResponses.add(CategoryResponse.convertCategoryResponse(c));
		}
		
		for (Comment cm : dbComments) {
			commentResponses.add(CommentResponse.convertCommentResponse(cm));
		}
		User dbu = b.getAuthor();
		BlogPostUserResponse bu = BlogPostUserResponse.convertBlogPostUserResponse(dbu);
		  BlogPostResponse resp = new BlogPostResponse(b.getTitle(), b.getContent(), bu, categoryResponses,
				commentResponses, b.getCreateAt(), b.getUpdateAt());
		  resp.setLikes(b.getLikes());
		  resp.setDislikes(b.getDislikes());
		  resp.setId(b.getId());
		  return resp;
	}

}
