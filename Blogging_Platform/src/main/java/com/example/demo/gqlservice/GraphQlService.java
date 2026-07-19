package com.example.demo.gqlservice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.EventDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.ReactDTO;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.BlockUnBlockException;
import com.example.demo.exception.FollowUnFollowException;
import com.example.demo.exception.InvalidIdException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.BlogPost;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.UserResponse;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Service
public class GraphQlService {

	@Autowired
	private BlogPostDao dao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private UserService userService;

	@Autowired
	private BlogPostService blogPostService;

	@Autowired
	private EventDao eventDao;
	@Autowired
	ObjectMapper objectMapper;

	Logger logger = LoggerFactory.getLogger(GraphQlService.class);

	@Transactional
	public List<BlogPostResponse> searchPosts(String keyword) {

		return dao.searchPosts(keyword).stream().map(BlogPostResponse::convertBlogPostRespons).toList();

	}

	// With pagination
	@Transactional
	public Page<BlogPostResponse> getPosts(int page, int size) {
		PageRequest pageable = PageRequest.of(page, size);
		Page<BlogPost> re = dao.findAll(pageable);
		return re.map(r -> BlogPostResponse.convertBlogPostRespons(r));
	}

	@Transactional
	public List<BlogPostResponse> getPinnedPostsOfTheUser(long uId) {

		try {
			if (userDao.findById(uId).isPresent()) {
				User u = userDao.findById(uId).get();
				logger.info("getting the pinned posts...");
				return u.getPinnedBlogPosts().stream().map(bp -> BlogPostResponse.convertBlogPostRespons(bp)).toList();
			} else {
				throw new ResourceNotFoundException("user is not valid");
			}

		} catch (Exception e) {
			logger.error("Exception occurred", e);
		    throw e;
		}

	}

	@Transactional
	public List<UserResponse> getFollowers(long uId) {
		if (uId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		return userDao.findById(uId).get().getListfollowers().stream().map(UserResponse::convertUserResponse).toList();

	}

	@Transactional
	public List<UserResponse> getFollowings(long uId) {
		if (uId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		return userDao.findById(uId).get().getListfollowing().stream().map(u -> UserResponse.convertUserResponse(u))
				.toList();

	}

	// top 10 most liked posts
	@Transactional
	public Page<BlogPostResponse> trendingPosts() {
		PageRequest pageable = PageRequest.of(0, 10);
		Page<BlogPost> re = dao.findTopNByOrderByLikesDesc(pageable);
		return re.map(r -> BlogPostResponse.convertBlogPostRespons(r));

	}

	@Transactional
	public List<BlogPostResponse> userLikedPost(long uId) {

		if (uId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}

		return userDao.findById(uId).get().getLikedBlogPosts().stream()
				.map(bp -> BlogPostResponse.convertBlogPostRespons(bp)).toList();

	}

	public List<UserResponse> getBlockedUsers(long uId) {
		if (uId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}

		User u = userDao.findById(uId).get();
		return u.getBlockedUsers().stream().map(b -> UserResponse.convertUserResponse(b)).toList();
	}

	// mutations

	public BlogPostResponse setReaction(ReactDTO request) throws Exception {
		
		try {
			Event event = new Event();
			BlogPost bp = blogPostService.getById(request.getBpId());
			User u = userService.getbyId((request.getuId()));
			// for like
			List<BlogPost> likedPosts = u.getLikedBlogPosts();
			Set<User> likingUsers = bp.getLikingUsers();

			// for dislike
			List<BlogPost> dislikedPosts = u.getDislikedBlogPosts();
			Set<User> dislikingUsers = bp.getDislikingUsers();

			// just remove the reaction if particular user has already reacted
			//double time clicking on like button or dislike button
			if ((request.isReaction() && likingUsers.contains(u))
					|| (!request.isReaction() && dislikingUsers.contains(u))) {
				if (likingUsers.contains(u)) {
					bp.setLikes(bp.getLikes() - 1);
					likedPosts.remove(bp);
					likingUsers.remove(u);

				} else {
					bp.setDislikes(bp.getDislikes() - 1);
					dislikedPosts.remove(bp);
					dislikingUsers.remove(u);

				}
				event.setEventType(EventType.REMOVE_REACT);
			} else {
				// for like
				if (request.isReaction() && !likingUsers.contains(u)) {
					// suppose 1st reaction was dislike, now want to like
					// so there will 2 steps operation - i)remove the reaction from the dislikes
					// list
					// and ii) add the like details in user and blogpost entity
					if (dislikingUsers.contains(u)) {
						bp.setDislikes(bp.getDislikes() - 1);
						dislikedPosts.remove(bp);
						dislikingUsers.remove(u);
					}

					likingUsers.add(u);
					if (bp.getLikes() == null) {
						bp.setLikes(1L);
					} else {
						bp.setLikes(bp.getLikes() + 1);
					}
					likedPosts.add(bp);
					event.setEventType(EventType.LIKE);
				} else {
					// for dislike
					if (!request.isReaction() && !dislikingUsers.contains(u)) {
						// previously liked the post now want to dislike
						if (likingUsers.contains(u)) {
							bp.setLikes(bp.getLikes() - 1);
							likedPosts.remove(bp);
							likingUsers.remove(u);
						}

						dislikingUsers.add(u);
						if (bp.getDislikes() == null) {
							bp.setDislikes(1L);
						} else {
							bp.setDislikes(bp.getDislikes() + 1);
						}
						dislikedPosts.add(bp);
						event.setEventType(EventType.DISLIKE);
					}

				}

			}

			userDao.save(u);
			dao.save(bp);
			BlogPostResponse updatedBlogPostResponse = BlogPostResponse.convertBlogPostRespons(bp);

			event.setCreatedAt(LocalDateTime.now());
			event.setPayload(objectMapper.writeValueAsString(request));
			event.setPublishedAt(LocalDateTime.now());
			event.setStatus(EventStatus.PENDING);
			event.setTransactionId(String.valueOf(updatedBlogPostResponse.getId()));
			event.setTransactionType(TransactionType.BLOGPOST);
			event.setRetryCount(0);
			eventDao.save(event);

			return updatedBlogPostResponse;

		} catch (ResourceNotFoundException e) {
			throw new ResourceNotFoundException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception occurred", e);
		    throw e;
		}

	}

	@Transactional
	public BlogPostResponse postPinnedUnpinned(long uId, long bpId) throws Exception {

		boolean b = false;

		if (uId <= 0 || bpId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}

		try {

			BlogPost bp = blogPostService.getById(bpId);
			User u = userService.getbyId(uId);

			if (bp.getPinnedBy().contains(u) && u.getPinnedBlogPosts().contains(bp)) {
				// wanted to unpin

				bp.getPinnedBy().remove(u);
				u.getPinnedBlogPosts().remove(bp);

			} else {
				// wanted to pin
				b = true;
				if (bp.getPinnedBy() == null) {
					bp.setPinnedBy(new HashSet<>());
				}
				bp.getPinnedBy().add(u);

				if (u.getPinnedBlogPosts() == null) {
					u.setPinnedBlogPosts(new HashSet<>());
				}
				u.getPinnedBlogPosts().add(bp);
				logger.info("Post has been pinned by user id: " + u.getId());

			}

			dao.save(bp);
			userDao.save(u);

			BlogPostResponse pinnedBlogPost = BlogPostResponse.convertBlogPostRespons(bp);

			Event event = new Event();
			if (b) {
				event.setEventType(EventType.PIN);
			} else {
				event.setEventType(EventType.UNPIN);
			}

			event.setCreatedAt(LocalDateTime.now());
			event.setPayload(objectMapper.writeValueAsString("User Id: "+uId+" BlogPost Id: "+bpId));
			event.setPublishedAt(LocalDateTime.now());
			event.setStatus(EventStatus.PENDING);
			event.setTransactionId(String.valueOf(pinnedBlogPost.getId()));
			event.setTransactionType(TransactionType.BLOGPOST);
			event.setRetryCount(0);
			eventDao.save(event);

			return pinnedBlogPost;
		} catch (ResourceNotFoundException exception) {
			throw new ResourceNotFoundException(exception.getMessage());
		} catch (Exception e) {
			logger.error("Exception occurred", e);
		    throw e;
		}

	}

	@Transactional
	public List<UserResponse> followOrUnFollowAuthor(long follower, long followee) throws Exception {

		if (followee <= 0 || followee <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		if (followee == follower) {
			throw new FollowUnFollowException("You can not follow yourself!");
		}

		try {
			boolean b = false;
			User followerUser = userService.getbyId(follower);
			User followeeUser = userService.getbyId(followee);

			// follower has blocked the followee -
			if (!followerUser.getBlockedUsers().contains(followeeUser)) {
				// trying to add into following set
				if (followerUser.getListfollowing().add(followeeUser)) {
					followeeUser.getListfollowers().add(followerUser);
					logger.info(follower + " has started following to " + followee);
					b = true;
				} else {
					// if hit the API with same values assuming wanna un-follow
					followerUser.getListfollowing().remove(followeeUser);
					followeeUser.getListfollowers().remove(followerUser);
					
				}
			} else {
				throw new FollowUnFollowException("You have blocked this user...");
			}

			followerUser.setFollowing((long) followerUser.getListfollowing().size());
			followeeUser.setFollowers((long) followeeUser.getListfollowers().size());
			User updatedFolloweeUser = userDao.save(followeeUser);
			User updatedFollowerUser = userDao.save(followerUser);

			List<User> users = new ArrayList<>(List.of(updatedFolloweeUser, updatedFollowerUser));

			Event event = new Event();
			if (b) {
				event.setEventType(EventType.FOLLOW);
			} else {
				event.setEventType(EventType.UNFOLLOW);
			}
			List<UserResponse> resp = users.stream().map(f -> UserResponse.convertUserResponse(f)).toList();
			event.setCreatedAt(LocalDateTime.now());
			event.setPayload(objectMapper.writeValueAsString("Follower Id: "+follower+" Followee Id: "+followee));
			event.setPublishedAt(LocalDateTime.now());
			event.setStatus(EventStatus.PENDING);
			event.setTransactionId(String.valueOf(follower));
			event.setTransactionType(TransactionType.USER);
			event.setRetryCount(0);
			eventDao.save(event);
			return resp;

		} catch (ResourceNotFoundException exception) {
			throw new ResourceNotFoundException(exception.getMessage());
		} catch (Exception e) {
			logger.error("Exception occurred", e);
		    throw e;
		}
	}

	public List<UserResponse> blockUnblockUser(Long blocker, Long blockedUser) throws Exception {

		if (blockedUser <= 0 || blocker <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		if (blocker == null || blockedUser == null) {
			throw new BlockUnBlockException("blockerId and blockedUserId must not be null");
		}

		if (blocker.equals(blockedUser)) {
			throw new BlockUnBlockException("blocker and blockedUser ids can not be same");
		}

		try {

			User b = userService.getbyId(blocker);
			User bu = userService.getbyId(blockedUser);
			Set<User> blockedUsersSet = b.getBlockedUsers();
			Set<User> blockedByUsersSet = bu.getBlockedByUsers();
			boolean bn = false;
			if (blockedUsersSet.contains(bu) && blockedByUsersSet.contains(b)) {
				// remove from block list
				blockedUsersSet.remove(bu);
				blockedByUsersSet.remove(b);

			} else {
				// want to block
				blockedUsersSet.add(bu);
				blockedByUsersSet.add(b);
				// blocked user got removed from list followings(if true)
				if (b.getListfollowing().contains(bu)) {
					Set<User> listfollowings = b.getListfollowing();
					listfollowings.remove(bu);
					b.setFollowing((long) listfollowings.size());

					Set<User> listfollowers = bu.getListfollowers();
					listfollowers.remove(b);
					bu.setFollowers((long) listfollowers.size());

				}
				bn = true;
			}
			User updatedBlocker = userDao.save(b);
			User updatedBlockedUser = userDao.save(bu);
			List<User> users = new ArrayList<>(List.of(updatedBlocker, updatedBlockedUser));

			Event event = new Event();
			if (bn) {
				event.setEventType(EventType.BLOCK);
			} else {
				event.setEventType(EventType.UNBLOCK);
			}
			List<UserResponse> resp = users.stream().map(f -> UserResponse.convertUserResponse(f)).toList();
			event.setCreatedAt(LocalDateTime.now());
			event.setPayload(objectMapper.writeValueAsString("Blocker Id: "+blocker+" BlockedUser Id: "+blockedUser));

			event.setPublishedAt(LocalDateTime.now());
			event.setStatus(EventStatus.PENDING);
			// who has blocked another user
			event.setTransactionId(String.valueOf(blocker));
			event.setTransactionType(TransactionType.USER);
			event.setRetryCount(0);
			eventDao.save(event);

			return resp;
		} catch (ResourceNotFoundException exception) {
			throw new ResourceNotFoundException("Either the blocker or the blocking user is not present!");
		} catch (Exception e) {
			logger.error("Exception occurred", e);
		    throw e;
		}

	}

}
