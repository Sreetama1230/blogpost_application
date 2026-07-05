package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.config.SecurityUtils;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.FeedItem;
import com.example.demo.model.BlogPost;
import com.example.demo.model.User;

import jakarta.transaction.Transactional;

@Service
public class FeedService {

	@Autowired
	private BlogPostDao blogPostDao;

	@Autowired
	private UserDao userDao;

	@Transactional
	public List<FeedItem> timeline( int start , int size ) {

		Long id = SecurityUtils.getCurrentUserId();
		if ( id == null ) {
			return loggedOutUser(start , size );
		}
		return loggedInUser(id, start , size );

	}

	public List<FeedItem> loggedInUser(long id, int start, int size) {

		List<FeedItem> feedItems = new ArrayList<FeedItem>();

		User loggedIn = userDao.findById(id).get();
		Set<User> followees = loggedIn.getListfollowing();

		List<Long> followeeIds = new ArrayList<>();
		for (User u : followees) {
			followeeIds.add(u.getId());
		}
		List<BlogPost> feedPosts = new ArrayList<>();

		if(followeeIds.isEmpty()) {
			return loggedOutUser(start , size);
		}
		feedPosts.addAll(blogPostDao.findPostsNotInListOfAuthor(followeeIds, PageRequest.of(start, size)));
		feedPosts.addAll(blogPostDao.findPostsInListAuthors(followeeIds, PageRequest.of(start, size)));

		// will return the final list based on count(like-dislike) and createAt at desc

		feedItems = feedPosts.stream().distinct()
				.sorted(Comparator.comparingLong(
						(BlogPost bp) -> bp.getLikes() - bp.getDislikes()).reversed()
						.thenComparing(BlogPost::getCreateAt , Comparator.reverseOrder()))
				.limit(size).map(FeedItem::convertToFeedItem).toList();

		return feedItems;

	}

	public List<FeedItem> loggedOutUser(int start, int size) {
		return findPopularPosts(start, size).stream().map(FeedItem::convertToFeedItem).toList();

	}

	public List<BlogPost> findPopularPosts(int start, int size) {
		// when a post is popular?
		// reactCount desc if same then post.createTime desc
		return blogPostDao.findPostByCreateTimeAndReactCount(PageRequest.of(start, size));

	}

}
