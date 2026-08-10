package com.example.demo.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

@Entity
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String name;

	@ManyToMany(mappedBy = "categories")
	private Set<BlogPost> blogPosts = new HashSet<>();

	@ManyToOne
	private User user;
	
	@Version
	private Long syncToken;
	
	
	public Set<BlogPost> getBlogPosts() {
		return blogPosts;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Category() {
		super();
	}

	public void setBlogPosts(HashSet<BlogPost> blogPosts) {
		this.blogPosts = blogPosts;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public Category(String name) {
		super();
		this.name = name;
	}

	public Long getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(Long syncToken) {
		this.syncToken = syncToken;
	}

	

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setBlogPosts(Set<BlogPost> blogPosts) {
		this.blogPosts = blogPosts;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Category other = (Category) obj;
		return Objects.equals(name, other.name);
	}
	

	public Category(String name, Set<BlogPost> blogPosts) {
		super();
		this.name = name;
		this.blogPosts = blogPosts;
	}

	public Category(String name, Set<BlogPost> blogPosts, User user) {
		super();
		this.name = name;
		this.blogPosts = blogPosts;
		this.user = user;
	}

	public Category(Long id, String name, Set<BlogPost> blogPosts, User user, Long syncToken) {
		super();
		this.id = id;
		this.name = name;
		this.blogPosts = blogPosts;
		this.user = user;
		this.syncToken = syncToken;
	}

	public Category(String name, Set<BlogPost> blogPosts, User user, Long syncToken) {
		super();
		this.name = name;
		this.blogPosts = blogPosts;
		this.user = user;
		this.syncToken = syncToken;
	}

	

}
