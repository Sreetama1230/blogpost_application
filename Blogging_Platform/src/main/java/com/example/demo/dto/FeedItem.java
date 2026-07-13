package com.example.demo.dto;

import com.example.demo.model.BlogPost;

public class FeedItem {

	private Long likeCount;
	private Long dislikeCount;
	private Long commentCount;
	private String authorUsername;
	private String blogDescription;
	private Long blogId;

	

	public Long getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Long likeCount) {
		this.likeCount = likeCount;
	}

	public Long getDislikeCount() {
		return dislikeCount;
	}

	public void setDislikeCount(Long dislikeCount) {
		this.dislikeCount = dislikeCount;
	}

	public Long getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(Long commentCount) {
		this.commentCount = commentCount;
	}

	

	public String getBlogDescription() {
		return blogDescription;
	}

	public void setBlogDescription(String blogDescription) {
		this.blogDescription = blogDescription;
	}

	public Long getBlogId() {
		return blogId;
	}

	public void setBlogId(Long blogId) {
		this.blogId = blogId;
	}

	public FeedItem() {
		super();
		// TODO Auto-generated constructor stub
	}

	
	public String getAuthorUsername() {
		return authorUsername;
	}

	public void setAuthorUsername(String authorUsername) {
		this.authorUsername = authorUsername;
	}
	
	


	public FeedItem(Long likeCount, Long dislikeCount, Long commentCount, String authorUsername, String blogDescription,
			Long blogId) {
		super();
		this.likeCount = likeCount;
		this.dislikeCount = dislikeCount;
		this.commentCount = commentCount;
		this.authorUsername = authorUsername;
		this.blogDescription = blogDescription;
		this.blogId = blogId;
	}

	public static FeedItem convertToFeedItem(BlogPost blogPost) {

		FeedItem feedItem = new FeedItem();
		feedItem.setAuthorUsername(blogPost.getAuthor().getUsername());
		feedItem.setBlogDescription(blogPost.getTitle());
		feedItem.setCommentCount(Long.valueOf(blogPost.getComments().size()));
		feedItem.setBlogId(blogPost.getId());
		feedItem.setLikeCount(blogPost.getLikes());
		feedItem.setDislikeCount(blogPost.getDislikes());
		return feedItem;
	}

}
