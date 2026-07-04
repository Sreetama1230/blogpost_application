package com.example.demo.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Category;

@Repository
public interface CategoryDao extends JpaRepository<Category, Long> {

	@Query("Select c from Category c where name = :n")
	Optional<Category> findByName( @Param(value="n") String n);

}
