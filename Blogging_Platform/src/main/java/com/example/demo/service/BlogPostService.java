package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.example.demo.config.SecurityUtils;
import com.example.demo.constants.AppConstants;
import com.example.demo.dao.CommentDao;
import com.example.demo.dao.UserDao;
import com.example.demo.exception.CategoryException;
import com.example.demo.exception.CustomExceptionHandler;
import com.example.demo.model.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;

import jakarta.transaction.Transactional;

@Service
public class BlogPostService {

	@Autowired
	private BlogPostDao blogPostDao;
	@Autowired
	private UserDao userDao;

	@Autowired
	private CommentDao commentDao;
	@Autowired
	private UserService userService;
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	Logger logger = LoggerFactory.getLogger(BlogPostService.class);

	public List<BlogPost> getAll() {
		return blogPostDao.findAll();
	}

	public BlogPost getById(long id) {
		if (blogPostDao.findById(id).isPresent()) {
			return blogPostDao.findById(id).get();
		} else {
			throw new ResourceNotFoundException("BlogPost is not present with this id");
		}
	}

	@Transactional
	public BlogPostResponse createOrUpdateBlogPost(BlogPostDTO bp) {
		long userId = SecurityUtils.getCurrentUserId();
		User u = userService.getbyId(userId);
		Set<CategoryDTO> dtos = bp.getCategories();
		HashSet<Category> catSet = new HashSet<>();
		List<BlogPost> blogPosts = u.getBlogPosts();
		BlogPost bpdata = new BlogPost();
		BlogPost newBlogPost = new BlogPost();

		// converting new categories
		for (CategoryDTO c : dtos) {

			try {
				StringBuilder categoryName = new StringBuilder();

				if (!c.getName().startsWith("#")) {
					categoryName.append("#");
					categoryName.append(c.getName());
				} else {
					categoryName.append(c.getName());
				}
				if (categoryService.getByName(categoryName.toString()) != null) { // exception will be thrown
					Category category = categoryService.getByName(categoryName.toString());
					catSet.add(category);
				}
			} catch (ResourceNotFoundException e) {
				logger.info("Adding new category...");
				Category newCat = categoryService.createCategory(new Category(c.getName(), new HashSet<>()));
				catSet.add(newCat);

			} catch (Exception e) {
				throw e;
			}

		}
		// update
		if (bp.getId() > 0 && blogPostDao.findById(bp.getId()).isPresent()) {
			BlogPost existingBP = blogPostDao.findById(bp.getId()).get();
			User blogpostAuthor = existingBP.getAuthor();
			User authDbUser = userDao.findById(userId).get();
			if (canUpdateOrDelete(authDbUser, blogpostAuthor)) {
				
				existingBP.setUpdateAt(LocalDateTime.now());
				// sparse update 
				if(bp.getContent() != null) {
					existingBP.setContent(bp.getContent());
				}
				
				// if not category is present then it will retain the old categories i.e. will not throw any exception
				if(bp.getCategories() != null) {
					existingBP.setCategories(catSet);
				}
				
				if(bp.getTitle() != null) {
					existingBP.setTitle(bp.getTitle());
				}
			
				bpdata = existingBP;
				newBlogPost = blogPostDao.save(existingBP);
				try {
					SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
							"Updated BlogPost " + String.valueOf(newBlogPost.getId())).get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
//				
			} else {
				throw new DoNotHavePermissionError("You can not do the update!");
			}

		} else {
			// Assuming while creating the blog post...it does not have any comments,likes
			// or dislikes etc

			// if no category is provided while creating the blog post
			if (dtos.isEmpty()) {
				throw new CategoryException("You have to specify a category to proceed");
			}

			BlogPost reqPost = new BlogPost(bp.getTitle(), bp.getContent(), u, catSet, new ArrayList<>(),
					LocalDateTime.now(), LocalDateTime.now());
			reqPost.setLikes(0L);
			reqPost.setDislikes(0L);

			
			newBlogPost = blogPostDao.save(reqPost);
			logger.info("blog post has been created  : ", reqPost.getContent());
			
			try {
				SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
						"Created BlogPost " + String.valueOf(newBlogPost.getId())).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			bpdata = reqPost;
			// adding the new blog post in the user's blogs list
			blogPosts.add(reqPost);
		}

		// Converting List to HashSet
		HashSet<BlogPost> hs = new HashSet<>();
		for (BlogPost b : blogPosts) {
			hs.add(b);
		}
		// updating categories ... setting categories
		for (Category upCat : catSet) {
			upCat.setBlogPosts(hs);
			categoryService.createCategory(upCat);
		}

		// updating the user
		u.setBlogPosts(blogPosts);
		u.setTotalPosts((long) u.getBlogPosts().size());
		logger.info("new blog post has beed added for {} ", u.getUsername());
		userDao.save(u);

		return BlogPostResponse.convertBlogPostRespons(bpdata);

	}

	@Transactional
	public BlogPostResponse deleteBlogPost(long id) {
		long userId = SecurityUtils.getCurrentUserId();
		BlogPostResponse deletedBlogPost = new BlogPostResponse();

		try {
			if (blogPostDao.findById(id).isPresent()) {

				BlogPost bp = blogPostDao.findById(id).get();
				User authDbUser = userDao.findById(userId).get();
				User blogpostAuthor = bp.getAuthor();
				if (canUpdateOrDelete(authDbUser, blogpostAuthor)) {
					deletedBlogPost = BlogPostResponse.convertBlogPostRespons(bp);
					// delete the comments as well
					for (Comment c : bp.getComments()) {
						commentDao.deleteById(c.getId());
					}
					deletedBlogPost.getComments().clear();
					blogPostDao.deleteById(id);
					try {
						SendResult<String, String> res = kafkaTemplate
								.send(AppConstants.ADMINTOOL_TOPIC_NAME, "Deleted BlogPost " + String.valueOf(id))
								.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					return deletedBlogPost;

				} else {
					throw new DoNotHavePermissionError("You are not the author of this post or an admin!");
				}
			} else {
				throw new ResourceNotFoundException("Resource is not found!");
			}
		} catch (DoNotHavePermissionError e) {
			throw new DoNotHavePermissionError(e.getMessage());
		} catch (ResourceNotFoundException e) {
			throw new ResourceNotFoundException(e.getMessage());
		} catch (Exception e) {
			throw new UnexpectedCustomException("Unexpected exception: " + e.getMessage());

		}

	}

	public List<BlogPostResponse> getBlogsByTitleAndUserId(String title, Long userId) {

		return blogPostDao.findByTitleAndAuthor(title, userId).stream().map(BlogPostResponse::convertBlogPostRespons)
				.toList();
	}

	public static boolean canUpdateOrDelete(User authDbUser, User blogpostAuthor) {
		if (SecurityUtils.isUser(authDbUser)) {
			return false;
		}
		// admin is removing some editor's blogposts
		if (SecurityUtils.isAdmin(authDbUser)) {
			return authDbUser.getId().equals(blogpostAuthor.getId()) || SecurityUtils.isEditor(blogpostAuthor);
		}

		if (SecurityUtils.isEditor(authDbUser)) {
			return authDbUser.getId().equals(blogpostAuthor.getId());
		}

		return false;
	}
}
