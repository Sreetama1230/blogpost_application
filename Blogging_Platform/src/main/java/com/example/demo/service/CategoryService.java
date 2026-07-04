package com.example.demo.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.demo.response.BlogPostResponse;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.demo.constants.AppConstants;
import com.example.demo.dao.CategoryDao;
import com.example.demo.dao.UserDao;
import com.example.demo.exception.CategoryLinkedToBlogs;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;
import com.example.demo.model.Category;

@Service
public class CategoryService {

	@Autowired
	private CategoryDao categoryDao;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	Logger logger = LoggerFactory.getLogger(CategoryService.class);

	@Transactional
	public Category createCategory(Category cg) {
		String s = cg.getName();
		String str = null;
		if (!(s.startsWith("#"))) {
			str = "#" + s;
			cg.setName(str);

		}
		Category newCategory = categoryDao.save(cg);

		CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
				"Created Category " + String.valueOf(newCategory.getId()));

		future.whenComplete((result, exception) -> {
			if (exception != null) {
				throw new UnexpectedCustomException("Error occured while publishing the event");
			} else {
				logger.info("Successfully published the event...");
			}
		});

		return newCategory;

	}

	public List<Category> getAll() {
		return categoryDao.findAll();
	}

	public Category getById(long id) {

		if (categoryDao.findById(id).isPresent()) {
			return categoryDao.findById(id).get();
		} else {
			throw new ResourceNotFoundException("Category with provided id is not present.");
		}

	}

	public Category getByName(String n) {

		if (categoryDao.findByName(n).isPresent()) {
			return categoryDao.findByName(n).get();
		} else {
			throw new ResourceNotFoundException("Category with provided name is not present");
		}
	}

	@Transactional
	public Category deleteById(long id) {

		if (categoryDao.findById(id).isPresent()) {
			Category c = categoryDao.findById(id).get();

			if (c.getBlogPosts().isEmpty()) {
				categoryDao.deleteById(id);

				CompletableFuture<SendResult<String, String>> future =

						kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
								"Deleted Category " + String.valueOf(c.getId()));

				future.whenComplete((result, exception) -> {
					if (exception != null) {
						throw new UnexpectedCustomException("Error occured while publishing the event");
					} else {
						logger.info("Successfully published the event...");
					}
				});

				return c;
			} else {
				// preferring not to delete in case of blogs are linked with the category!
				throw new CategoryLinkedToBlogs("Some Blogs are linked with this category!..can not be deleted!");
			}

		} else {
			throw new ResourceNotFoundException("Category with provided id is not present.");
		}

	}

	public List<BlogPostResponse> listBlogsByCategory(String categoryName) {

		String normalizedName = categoryName.startsWith("#") ? categoryName : "#" + categoryName;

		try {

			return categoryDao.findByName(normalizedName).get().getBlogPosts().stream()
					.map(BlogPostResponse::convertBlogPostRespons).toList();

		} catch (ResourceNotFoundException e) {
			throw new ResourceNotFoundException("Category with provided name is not present.");
		} catch (Exception e) {
			throw e;
		}

	}
}
