package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;



@Entity
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column
	private String content;

	@ManyToOne
	private User user;
	@ManyToOne
	private BlogPost blogPost;

	@CreatedDate
	private LocalDateTime createAt;
	
	@Column
	private Long likeCount;
	@Column
	private Long loveCount;
	@Column
	private Long funnyCount;

    @ManyToMany
    @JoinTable(
        name = "comment_reactions",
        joinColumns = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> reactedUsers = new HashSet<>();
	
	public Long getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Long likeCount) {
		this.likeCount = likeCount;
	}

	public Long getLoveCount() {
		return loveCount;
	}

	public void setLoveCount(Long loveCount) {
		this.loveCount = loveCount;
	}

	public Long getFunnyCount() {
		return funnyCount;
	}

	public void setFunnyCount(Long funnyCount) {
		this.funnyCount = funnyCount;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public BlogPost getBlogPost() {
		return blogPost;
	}

	public void setBlogPost(BlogPost blogPost) {
		this.blogPost = blogPost;
	}

	public LocalDateTime getCreateAt() {
		return createAt;
	}

	public void setCreateAt(LocalDateTime createAt) {
		this.createAt = createAt;
	}

	public Comment() {
		super();
		// TODO Auto-generated constructor stub
	}


	


	public Comment(String content, User user, BlogPost blogPost, LocalDateTime createAt, Long likeCount, Long loveCount,
			Long funnyCount, Set<User> reactedUsers) {
		super();
		this.content = content;
		this.user = user;
		this.blogPost = blogPost;
		this.createAt = createAt;
		this.likeCount = likeCount;
		this.loveCount = loveCount;
		this.funnyCount = funnyCount;
		this.reactedUsers = reactedUsers;
	}

	public Comment(Long id, String content, User user, BlogPost blogPost, LocalDateTime createAt, Long likeCount,
			Long loveCount, Long funnyCount, Set<User> reactedUsers) {
		super();
		this.id = id;
		this.content = content;
		this.user = user;
		this.blogPost = blogPost;
		this.createAt = createAt;
		this.likeCount = likeCount;
		this.loveCount = loveCount;
		this.funnyCount = funnyCount;
		this.reactedUsers = reactedUsers;
	}

	public Set<User> getReactedUsers() {
		return reactedUsers;
	}

	public void setReactedUsers(Set<User> reactedUsers) {
		this.reactedUsers = reactedUsers;
	}

	@Override
	public int hashCode() {
		return Objects.hash(content);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Comment other = (Comment) obj;
		return Objects.equals(content, other.content);
	}

}
