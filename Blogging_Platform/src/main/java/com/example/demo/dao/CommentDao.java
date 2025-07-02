package com.example.demo.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Comment;

@Repository
public interface CommentDao extends JpaRepository<Comment, Long> {

	@Query( value= "SELECT c FROM Comment c WHERE c.id = ?1 AND c.blogPost.id = ?2")
	public   Optional< Comment> findCommentByIdAndBlogPostId(long id, long blogPost);
}
