package com.example.demo.model;

import java.util.*;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column
	private String name;
	@ManyToMany(mappedBy = "categories")
	private Set<BlogPost> blogPosts = new HashSet<>();

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
		// TODO Auto-generated constructor stub
	}

	public Category(String name, Set<BlogPost> blogPosts) {
		super();
		this.name = name;
		this.blogPosts = blogPosts;
	}

	public void setBlogPosts(HashSet<BlogPost> blogPosts) {
		this.blogPosts = blogPosts;
	}


	@Override
	public int hashCode() {
		return Objects.hash(name);
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



}
