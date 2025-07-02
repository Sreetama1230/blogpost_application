package com.example.demo.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Category;

@Repository
public interface CategoryDao extends JpaRepository<Category, Long> {

	
	Optional<Category> findByName(String n);

}
