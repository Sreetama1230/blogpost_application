package com.example.demo.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.BlogPost;

@Repository
public interface BlogPostDao extends JpaRepository<BlogPost, Long> {

	@Query(value = "Select b from BlogPost b where b.content Like concat('%',:keyword,'%') or b.title Like concat('%',:keyword,'%')")
	public List<BlogPost> searchPosts(@Param(value = "keyword") String keyword);

	public Page<BlogPost> findAll(Pageable pageable);

	@Query("SELECT bp FROM BlogPost bp ORDER BY bp.likes DESC")
	public Page<BlogPost> findTopNByOrderByLikesDesc(Pageable pageable);

	@Query("SELECT bp FROM BlogPost bp WHERE bp.title = :title AND bp.author.id = :authorId")
	public List<BlogPost> findByTitleAndAuthor(@Param(value = "title") String title,
			@Param(value = "authorId") Long id);

	@Query("SELECT bp FROM BlogPost bp ORDER BY (bp.likes- bp.dislikes) DESC, bp.createAt DESC ")
	public List<BlogPost> findPostByCreateTimeAndReactCount(Pageable page);

	@Query("""
			SELECT bp FROM BlogPost bp
			WHERE bp.author.id IN :authorIds
			ORDER BY (bp.likes- bp.dislikes) DESC, bp.createAt DESC
			""")
	public List<BlogPost> findPostsInListAuthors(@Param("authorIds") List<Long> authorIds, Pageable page);

	@Query("""
			SELECT bp FROM BlogPost bp
			WHERE bp.author.id NOT IN :authorIds
			ORDER BY (bp.likes- bp.dislikes) DESC, bp.createAt DESC
			""")
	public List<BlogPost> findPostsNotInListOfAuthor(@Param("authorIds") List<Long> authorIds, Pageable page);
}
