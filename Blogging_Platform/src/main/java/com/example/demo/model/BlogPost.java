package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

@Entity
public class BlogPost {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column
	private String title;
	@Column
	private String content;
	
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String image;
	

	@ManyToOne
	private User author;
	
	@ManyToMany(mappedBy = "pinnedBlogPosts")
	private Set<User> pinnedBy = new HashSet<>();

	@Column
	private  Long likes;
	
	@Column
	private  Long dislikes;

	@Version
	private Long syncToken;
	
	@CreatedDate
	private LocalDateTime createAt;
	@LastModifiedDate
	private LocalDateTime updateAt;
	
	
	@ManyToMany
	@JoinTable(

			name = "post_categories", joinColumns = @JoinColumn(name = "post_id"),
			inverseJoinColumns = @JoinColumn(name = "categories_id")

	)
	private Set<Category> categories= new HashSet<>();; 

	@OneToMany(mappedBy = "blogPost")
	private List<Comment> comments= new ArrayList<>();
	
	@ManyToMany(mappedBy = "likedBlogPosts")
	private Set<User> likingUsers = new HashSet<>();
	
	@ManyToMany(mappedBy = "dislikedBlogPosts")
	private Set<User> dislikingUsers = new HashSet<>();
	
	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
  
	public Set<User> getDislikingUsers() {
		return dislikingUsers;
	}


	public void setDislikingUsers(Set<User> dislikingUsers) {
		this.dislikingUsers = dislikingUsers;
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

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	public Set<Category> getCategories() {
		return categories;
	}


	public List<Comment> getComments() {
		return comments;
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


	public BlogPost(String title, String content, User author, Set<Category> categories, List<Comment> comments,
			LocalDateTime createAt, LocalDateTime updateAt, Long likes) {
		super();
		this.title = title;
		this.content = content;
		this.author = author;
		this.categories = categories;
		this.comments = comments;
		this.createAt = createAt;
		this.updateAt = updateAt;
		this.likes = likes;
	}

	public Long getLikes() {
		return likes;
	}

	
	public Set<User> getLikingUsers() {
		return likingUsers;
	}

	public void setLikingUsers(Set<User> likingUsers) {
		this.likingUsers = likingUsers;
	}

	public void setLikes(Long likes) {
		this.likes = likes;
	}

	public BlogPost(String title, String content, User author, Set<Category> categories, List<Comment> comments,
			LocalDateTime createAt, LocalDateTime updateAt) {
		super();
		this.title = title;
		this.content = content;
		this.author = author;
		this.categories = categories;
		this.comments = comments;
		this.createAt = createAt;
		this.updateAt = updateAt;
	}

	public void setCategories(Set<Category> categories) {
		this.categories = categories;
	}


	public BlogPost() {
		super();
		// TODO Auto-generated constructor stub
	}

	

	public Set<User> getPinnedBy() {
		return pinnedBy;
	}

	public void setPinnedBy(Set<User> pinnedBy) {
		this.pinnedBy = pinnedBy;
	}

	public Long getDislikes() {
		return dislikes;
	}

	public void setDislikes(Long dislikes) {
		this.dislikes = dislikes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(author, categories, content, createAt, title, updateAt);
	}
	
	
	

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public Long getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlogPost other = (BlogPost) obj;
		return Objects.equals(author, other.author) && Objects.equals(categories, other.categories)
				&& Objects.equals(content, other.content) && Objects.equals(createAt, other.createAt)
				&& Objects.equals(title, other.title) && Objects.equals(updateAt, other.updateAt);
	}

	public BlogPost(String title, String content, User author, Set<User> pinnedBy, Long likes, Long dislikes,
			Long syncToken, LocalDateTime createAt, LocalDateTime updateAt, Set<Category> categories,
			List<Comment> comments, Set<User> likingUsers, Set<User> dislikingUsers) {
		super();
		this.title = title;
		this.content = content;
		this.author = author;
		this.pinnedBy = pinnedBy;
		this.likes = likes;
		this.dislikes = dislikes;
		this.syncToken = syncToken;
		this.createAt = createAt;
		this.updateAt = updateAt;
		this.categories = categories;
		this.comments = comments;
		this.likingUsers = likingUsers;
		this.dislikingUsers = dislikingUsers;
	}
	
	

}
