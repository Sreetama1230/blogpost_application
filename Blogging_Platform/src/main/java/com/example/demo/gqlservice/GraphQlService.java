package com.example.demo.gqlservice;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.example.demo.exception.BlockUnBlockException;
import com.example.demo.exception.FollowUnFollowException;
import com.example.demo.exception.InvalidIdException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.demo.constants.AppConstants;
import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.ReactDTO;
import com.example.demo.model.BlogPost;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.PinnedBlogPost;
import com.example.demo.response.UserResponse;
import com.example.demo.service.BlogPostService;
import com.example.demo.service.UserService;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnexpectedCustomException;

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
	private KafkaTemplate<String, String> kafkaTemplate;

	Logger logger = LoggerFactory.getLogger(GraphQlService.class);

	public List<BlogPostResponse> searchPosts(String keyword) {

		return dao.searchPosts(keyword).stream().map(BlogPostResponse::convertBlogPostRespons).toList();

	}

	// With pagination
	public Page<BlogPostResponse> getPosts(int page, int size) {
		PageRequest pageable = PageRequest.of(page, size);
		Page<BlogPost> re = dao.findAll(pageable);
		return re.map(r -> BlogPostResponse.convertBlogPostRespons(r));
	}

	public List<PinnedBlogPost> getPinnedPostsOfTheUser(long uId) {

		try {
			if (userDao.findById(uId).isPresent()) {
				User u = userDao.findById(uId).get();
				logger.info("getting the pinned posts...");
				return u.getPinnedBlogPosts().stream().map(bp -> PinnedBlogPost.convertPinnedBlogPosts(bp)).toList();
			} else {
				throw new ResourceNotFoundException("user is not valid");
			}

		} catch (Exception e) {
			throw e;
		}

	}

	public List<UserResponse> getFollowers(long uId) {
		if (uId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		return userDao.findById(uId).get().getListfollowers().stream().map(UserResponse::convertUserResponse).toList();

	}

	public List<UserResponse> getFollowings(long uId) {
		if (uId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		return userDao.findById(uId).get().getListfollowing().stream().map(u -> UserResponse.convertUserResponse(u))
				.toList();

	}

	// top 10 most liked posts
	public Page<BlogPostResponse> trendingPosts() {
		PageRequest pageable = PageRequest.of(0, 10);
		Page<BlogPost> re = dao.findTopNByOrderByLikesDesc(pageable);
		return re.map(r -> BlogPostResponse.convertBlogPostRespons(r));

	}

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

	public BlogPostResponse setReaction(ReactDTO request) {

		try {
			BlogPost bp = blogPostService.getById(request.getBpId());
			User u = userService.getbyId((request.getuId()));
			// for like
			List<BlogPost> likedPosts = u.getLikedBlogPosts();
			Set<User> likingUsers = bp.getLikingUsers();

			// for dislike
			List<BlogPost> dislikedPosts = u.getDislikedBlogPosts();
			Set<User> dislikingUsers = bp.getDislikingUsers();

			// just remove the reaction if particular user has already reacted
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
					}

				}

			}

			userDao.save(u);
			dao.save(bp);

			SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
					"User " + String.valueOf(u.getId()) + "has reacted to the post id " + bp.getId()).get();

			return BlogPostResponse.convertBlogPostRespons(bp);

		} catch (ResourceNotFoundException e) {
			throw new ResourceNotFoundException(e.getMessage());
		} catch (Exception e) {
			throw new UnexpectedCustomException(e.getMessage());
		}

	}

	public PinnedBlogPost pinnedPost(long uId, long bpId) {

		if (uId <= 0 || bpId <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}

		try {
			BlogPost bp = blogPostService.getById(bpId);
			User u = userService.getbyId(uId);

			bp.setPinnedDate(LocalDateTime.now());
			bp.setPinnedBy(u);
			logger.info("pinning a post ");
			dao.save(bp);

			List<BlogPost> pinnedPosts = u.getPinnedBlogPosts();
			if (u.getPinnedBlogPosts() == null) {
				pinnedPosts = new ArrayList<>();
			}

			pinnedPosts.add(bp);

			SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
					"User :  " + String.valueOf(u.getId()) + "has pinned post : " + bp.getId()).get();

			return PinnedBlogPost.convertPinnedBlogPosts(bp);
		} catch (ResourceNotFoundException exception) {
			throw new ResourceNotFoundException(exception.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new UnexpectedCustomException(e.getMessage());
		}

	}

	public List<UserResponse> followOrUnFollowAuthor(long follower, long followee) {

		if (followee <= 0 || followee <= 0) {
			throw new InvalidIdException("Invalid ID!");
		}
		if (followee == follower) {
			throw new FollowUnFollowException("You can not follow yourself!");
		}

		logger.info(follower + " has started following to " + followee);
		try {
			User followerUser = userService.getbyId(follower);
			User followeeUser = userService.getbyId(followee);
			if (!followerUser.getBlockedUsers().contains(followeeUser)) {
				if (followerUser.getListfollowing().add(followeeUser)) {
					followeeUser.getListfollowers().add(followerUser);

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
			userDao.save(followeeUser);
			userDao.save(followerUser);

			SendResult<String, String> res = kafkaTemplate
					.send(AppConstants.ADMINTOOL_TOPIC_NAME,
							"User id " + String.valueOf(follower + " has started following to the user id " + followee))
					.get();

			List<User> users = new ArrayList<>(List.of(followeeUser, followerUser));
			return users.stream().map(f -> UserResponse.convertUserResponse(f)).toList();

		} catch (ResourceNotFoundException exception) {
			throw new ResourceNotFoundException(exception.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new UnexpectedCustomException(e.getMessage());
		}
	}

	public List<UserResponse> blockUser(Long blocker, Long blockedUser) {

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
			}
			userDao.save(b);
			userDao.save(bu);
			List<User> users = new ArrayList<>(List.of(b, bu));

			try {
				SendResult<String, String> res = kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
						"User id " + String.valueOf(b.getId() + " has blocked user id " + bu.getId())).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			return users.stream().map(u -> UserResponse.convertUserResponse(u)).toList();
		} catch (ResourceNotFoundException exception) {
			throw new ResourceNotFoundException("Either the blocker or the blocking user is not present!");
		} catch (Exception e) {
			throw e;
		}

	}

}
