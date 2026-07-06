package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.example.demo.config.SecurityUtils;
import com.example.demo.constants.AppConstants;
import com.example.demo.dao.*;
import com.example.demo.dto.UserDTO;
import com.example.demo.exception.CategoryException;
import com.example.demo.exception.CustomExceptionHandler;
import com.example.demo.model.Comment;
import com.example.demo.model.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.DoNotHavePermissionError;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Category;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private EventDao eventDao;
	@Autowired
	private ObjectMapper objectMapper;

    @Autowired
    private CategoryDao categoryDao;

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
    public BlogPostResponse createOrUpdateBlogPost(BlogPostDTO bp) throws JsonProcessingException {

        Event event = new Event();
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

                Event subEvent = new Event();

                subEvent.setEventType(EventType.CREATE);
                subEvent.setCreatedAt(LocalDateTime.now());
                subEvent.setPayload(objectMapper.writeValueAsString(c));
                subEvent.setPublishedAt(LocalDateTime.now());
                subEvent.setStatus(EventStatus.PENDING);
                subEvent.setTransactionId(String.valueOf(newBlogPost.getId()));
                subEvent.setTransactionType(TransactionType.CATEGORY);
                subEvent.setRetryCount(0);
                eventDao.save(subEvent);

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
                if (bp.getContent() != null) {
                    existingBP.setContent(bp.getContent());
                }

                // if not category is present then it will retain the old categories i.e. will
                // not throw any exception
                if (bp.getCategories() != null) {
                    existingBP.setCategories(catSet);
                }

                if (bp.getTitle() != null) {
                    existingBP.setTitle(bp.getTitle());
                }

                bpdata = existingBP;
                newBlogPost = blogPostDao.save(existingBP);

                event.setEventType(EventType.UPDATE);


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
            event.setEventType(EventType.CREATE);
            logger.info("blog post has been created  : ", reqPost.getContent());
            bpdata = newBlogPost;
            // adding the new blog post in the user's blogs list
            blogPosts.add(newBlogPost);
        }

        // Converting List to HashSet
        HashSet<BlogPost> hs = new HashSet<>();
        hs.addAll(blogPosts);
        // updating categories ... setting categories
        for (Category upCat : catSet) {
            upCat.setBlogPosts(hs);
            categoryDao.save(upCat);
        }


        // updating the user
        u.setBlogPosts(blogPosts);
        u.setTotalPosts((long) u.getBlogPosts().size());
        logger.info("new blog post has beed added for {} ", u.getUsername());
        userDao.save(u);


        // need to publish the user update event
        Event subEvent1 = new Event();

        subEvent1.setEventType(EventType.UPDATE);
        subEvent1.setCreatedAt(LocalDateTime.now());
        subEvent1.setPayload(objectMapper.writeValueAsString("Updated User while creating the blogpost: " + u.getId()));
        subEvent1.setPublishedAt(LocalDateTime.now());
        subEvent1.setStatus(EventStatus.PENDING);
        subEvent1.setTransactionId(String.valueOf(u.getId()));
        subEvent1.setTransactionType(TransactionType.USER);
        subEvent1.setRetryCount(0);
        eventDao.save(subEvent1);


        // blogpost  event
        event.setCreatedAt(LocalDateTime.now());
        event.setPayload(objectMapper.writeValueAsString(bp));
        event.setPublishedAt(LocalDateTime.now());
        event.setStatus(EventStatus.PENDING);
        event.setTransactionId(String.valueOf(newBlogPost.getId()));
        event.setTransactionType(TransactionType.BLOGPOST);
        event.setRetryCount(0);
        eventDao.save(event);

        return BlogPostResponse.convertBlogPostRespons(bpdata);

    }

	@Transactional
	public BlogPostResponse deleteBlogPost(long id) throws Exception {

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

					

					Event event = new Event();

					event.setEventType(EventType.DELETE);
					event.setCreatedAt(LocalDateTime.now());
					event.setPayload(objectMapper.writeValueAsString(id));
					event.setPublishedAt(LocalDateTime.now());
					event.setStatus(EventStatus.PENDING);
					event.setTransactionId(String.valueOf(bp.getId()));
					event.setTransactionType(TransactionType.BLOGPOST);
					event.setRetryCount(0);
					eventDao.save(event);
					
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
			throw e;

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
