package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.demo.response.BlogPostResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.demo.constants.AppConstants;
import com.example.demo.dao.CategoryDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.CategoryLinkedToBlogs;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;
import com.example.demo.model.Category;
import com.example.demo.model.Event;

@Service
public class CategoryService {

	@Autowired
	private CategoryDao categoryDao;

	@Autowired
	private EventDao eventDao;
	@Autowired
	private ObjectMapper objectMapper;

	Logger logger = LoggerFactory.getLogger(CategoryService.class);

	@Transactional
	public Category createCategory(Category cg) throws JsonProcessingException {
		String s = cg.getName();
		String str = null;
		if (!(s.startsWith("#"))) {
			str = "#" + s;
			cg.setName(str);

		}
		Category newCategory = categoryDao.save(cg);
	
		Event event = new Event();

		event.setEventType(EventType.CREATE);
		event.setCreatedAt(LocalDateTime.now());
		event.setPayload(objectMapper.writeValueAsString(cg));
		event.setPublishedAt(LocalDateTime.now());
		event.setStatus(EventStatus.PENDING);
		event.setTransactionId(String.valueOf(newCategory.getId()));
		event.setTransactionType(TransactionType.CATEGORY);
		event.setRetryCount(0);
		eventDao.save(event);

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
	public Category deleteById(long id) throws JsonProcessingException {

		if (categoryDao.findById(id).isPresent()) {
			Category c = categoryDao.findById(id).get();

			if (c.getBlogPosts().isEmpty()) {
				categoryDao.deleteById(id);

				
				Event event = new Event();

				event.setEventType(EventType.DELETE);
				event.setCreatedAt(LocalDateTime.now());
				event.setPayload(objectMapper.writeValueAsString(id));
				event.setPublishedAt(LocalDateTime.now());
				event.setStatus(EventStatus.PENDING);
				event.setTransactionId(String.valueOf(c.getId()));
				event.setTransactionType(TransactionType.CATEGORY);
				event.setRetryCount(0);
				eventDao.save(event);

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
