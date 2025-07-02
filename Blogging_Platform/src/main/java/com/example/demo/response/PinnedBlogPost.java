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

public class PinnedBlogPost {

	
	private LocalDateTime pinnedDate;
	
    private   BlogPostResponse blogPostResponse;

	
	public PinnedBlogPost(LocalDateTime pinnedDate, BlogPostResponse blogPostResponse) {
		super();
		this.pinnedDate = pinnedDate;
		this.blogPostResponse = blogPostResponse;
	}


	public LocalDateTime getPinnedDate() {
		return pinnedDate;
	}


	public void setPinnedDate(LocalDateTime pinnedDate) {
		this.pinnedDate = pinnedDate;
	}


	public BlogPostResponse getBlogPostResponse() {
		return blogPostResponse;
	}


	public void setBlogPostResponse(BlogPostResponse blogPostResponse) {
		this.blogPostResponse = blogPostResponse;
	}


	public static PinnedBlogPost convertPinnedBlogPosts(BlogPost b) {
		
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
		BlogPostResponse bpr=    new BlogPostResponse(  b.getId() ,b.getTitle(), b.getContent(), bu, categoryResponses,
				commentResponses, b.getCreateAt(), b.getUpdateAt());
		
		return new PinnedBlogPost(b.getPinnedDate(),bpr);
	}
	
	
}
