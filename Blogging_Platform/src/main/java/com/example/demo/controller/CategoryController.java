package com.example.demo.controller;

import java.util.*;

import com.example.demo.exception.CategoryException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.response.BlogPostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.CategoryDTO;
import com.example.demo.model.Category;
import com.example.demo.response.CategoryResponse;
import com.example.demo.service.CategoryService;

@RestController
@RequestMapping("/categories")
public class CategoryController {
	
	@Autowired
	private CategoryService service;
	
	Logger logger = LoggerFactory.getLogger(CategoryController.class);
	
	@PostMapping
	public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryDTO cdto){
		Category c = new Category();
		String name = cdto.getName();
		String normalizedName = name.startsWith("#") ? name : "#" + name;
//		if(cdto.getId() != null && cdto.getId()>0){
//		  throw new CategoryException("You can not change an existing category! please create new one by removing the id");
//
//		}else {
			try {
				c = service.getByName(normalizedName);
				throw new CategoryException("Category is already present with the provided name...");
			} catch (ResourceNotFoundException e) {
				c = new Category(cdto.getName(), new HashSet<>());
				logger.info("created category : {} ", c.getName());
				service.createCategory(c);
				return new ResponseEntity<CategoryResponse>(CategoryResponse.convertCategoryResponse(c), HttpStatus.CREATED);
			}

	}

	@GetMapping
	public ResponseEntity<List<CategoryResponse>> getAll(){
		List<CategoryResponse> categories = new ArrayList<>();
		List<Category> DBCategories = service.getAll();
		for (Category c : DBCategories) {
			categories.add(CategoryResponse.convertCategoryResponse(c));
		}
		logger.info("getting all categories");
		return new ResponseEntity<List<CategoryResponse>>(categories,HttpStatus.OK);
	}

	@GetMapping("/name")
	public  ResponseEntity<List<BlogPostResponse>> listBlogsByCategory(  @RequestParam String name){
		return  new ResponseEntity<>(service.listBlogsByCategory(name) , HttpStatus.OK);

	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Category> deleteById(    @PathVariable Long id ){
			return new ResponseEntity<>(service.deleteById(id),HttpStatus.OK);
    }
}
