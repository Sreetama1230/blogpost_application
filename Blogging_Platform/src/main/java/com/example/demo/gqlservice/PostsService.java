package com.example.demo.gqlservice;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;


import com.example.demo.exception.FollowUnFollowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.dao.BlogPostDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.ReactDTO;
import com.example.demo.model.BlogPost;
import com.example.demo.model.User;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.PinnedBlogPost;
import com.example.demo.response.UserResponse;


import com.example.demo.exception.ResourceNotFoundException;

@Service
public class PostsService {

	@Autowired
	private BlogPostDao dao;

	@Autowired
	private UserDao userDao;

	Logger logger = LoggerFactory.getLogger(PostsService.class);

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
		logger.info("getting the pinned posts...");
		try {
			User u = userDao.findById(uId).get();
			return u.getPinnedBlogPosts().stream().map(bp -> PinnedBlogPost.convertPinnedBlogPosts(bp)).toList();
		} catch (Exception e) {
			throw new ResourceNotFoundException(e.getMessage());
		}

	}

	public List<UserResponse> getFollowers(long uId) {
		return userDao.findById(uId).get().getListfollowers().stream().map(UserResponse::convertUserResponse)
				.toList();

	}

	public List<UserResponse> getFollowings(long uId) {
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

		return userDao.findById(uId).get().getLikedBlogPosts().stream()
				.map(bp -> BlogPostResponse.convertBlogPostRespons(bp)).toList();

	}

	public List<UserResponse> getBlockedUsers(long uId) {
		User u = userDao.findById(uId).get();
		//System.out.println("ROLES : "+u.getRoles());
		return u.getBlockedUsers().stream().map(b -> UserResponse.convertUserResponse(b)).toList();
	}

	// mutations

	public BlogPostResponse setReaction(ReactDTO request) {

		try {
			BlogPost bp = dao.findById(request.getBpId()).get();
			User u = userDao.findById(request.getuId()).get();
			//for like
			List<BlogPost> likedPosts = u.getLikedBlogPosts();
			Set<User> likingUsers = bp.getLikingUsers();
			
			// for dislike
			List<BlogPost> dislikedPosts = u.getDislikedBlogPosts();
			Set<User> dislikingUsers = bp.getDislikingUsers();

			//just remove the reaction
			if((request.isReaction() &&  likingUsers.contains(u)) ||
					( !request.isReaction() &&  dislikingUsers.contains(u)))
			{
				if(likingUsers.contains(u)) {
					bp.setLikes(bp.getLikes()-1);
					likedPosts.remove(bp);
					likingUsers.remove(u);

				}else {
					bp.setDislikes(bp.getDislikes()-1);
					dislikedPosts.remove(bp);
					dislikingUsers.remove(u);

				}
			}else{
				// for like
				if (request.isReaction() && !likingUsers.contains(u)) {
					// suppose 1st reaction was dislike, now want do like
					// so there will 2 steps operation - i)remove the reaction from the dislikes list
					// and ii) add the like details in user and blogpost entity
						if(dislikingUsers.contains(u)){
							bp.setDislikes(bp.getDislikes()-1);
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
				}else {
					// for dislike
					if(!request.isReaction() &&  !dislikingUsers.contains(u)) {
						// previously liked the post now want to dislike
						if(likingUsers.contains(u)){
							bp.setLikes(bp.getLikes()-1);
							likedPosts.remove(bp);
							likingUsers.remove(u);
						}

						dislikingUsers.add(u);
						if(bp.getDislikes() == null) {
							bp.setDislikes(1L);
						}else {
							bp.setDislikes(bp.getDislikes()+1);
						}
						dislikedPosts.add(bp);
					}

				}

			}

			userDao.save(u);			
			dao.save(bp);
			
			return BlogPostResponse.convertBlogPostRespons(bp);

		} catch (Exception e) {
			throw new ResourceNotFoundException(e.getMessage());
		}

	}

	public PinnedBlogPost pinnedPost(long uId, long bpId) {
		logger.info("pinning a post ");
		try {
			BlogPost bp = dao.findById(bpId).get();
			User u = userDao.findById(uId).get();
			bp.setPinnedDate(LocalDateTime.now());
			bp.setPinnedBy(u);
			dao.save(bp);

			List<BlogPost> pinnedPosts=  u.getPinnedBlogPosts();
			if(u.getPinnedBlogPosts() == null){
				pinnedPosts = new ArrayList<>();
			}

			pinnedPosts.add(bp);

			return PinnedBlogPost.convertPinnedBlogPosts(bp);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new ResourceNotFoundException(e.getMessage());
		}

	}

	public List<UserResponse> followOrUnFollowAuthor(long follower, long followee) {

		if (followee == follower) {
			throw new FollowUnFollowException("You can not follow yourself!");
		}

		logger.info(follower + " has started following to " + followee);
		try {
			User followerUser = userDao.findById(follower).get();
			User followeeUser = userDao.findById(followee).get();
			if( ! followerUser.getBlockedUsers().contains(followeeUser)){
				if (followerUser.getListfollowing().add(followeeUser)) {
					followeeUser.getListfollowers().add(followerUser);

				} else {
					// if hit the API with same values assuming wanna  un-follow
					followerUser.getListfollowing().remove(followeeUser);
					followeeUser.getListfollowers().remove(followerUser);
				}
			}else{
				throw new FollowUnFollowException("You have blocked this user...");
			}


			followerUser.setFollowing((long) followerUser.getListfollowing().size());
			followeeUser.setFollowers((long) followeeUser.getListfollowers().size());
			userDao.save(followeeUser);
			userDao.save(followerUser);
			List<User> users = new ArrayList<>(List.of(followeeUser, followerUser));
			return users.stream().map(f -> UserResponse.convertUserResponse(f)).toList();

		} catch (Exception e) {
			throw new ResourceNotFoundException(e.getMessage());
		}

	}


	public List<UserResponse> blockUser(long blocker, long blockedUser) {
		
	
		if(userDao.findById(blocker).isPresent() && userDao.findById(blockedUser).isPresent() ) {
			User b = userDao.findById(blocker).get();
			User bu = userDao.findById(blockedUser).get();
			Set<User> blockedUsersSet = b.getBlockedUsers();
			Set<User> blockedByUsersSet = bu.getBlockedByUsers();
			
			if (blockedUsersSet.contains(bu) && blockedByUsersSet.contains(b)) {
				//remove from block list
				blockedUsersSet.remove(bu);	
				blockedByUsersSet.remove(b);
				
				
			} else {
				//want to block
				blockedUsersSet.add(bu);
				blockedByUsersSet.add(b);
				// blocked user got removed from list followings(if true)
				if (b.getListfollowing().contains(bu)) {
					Set<User> listfollowings = b.getListfollowing();
					listfollowings.remove(bu);
					b.setFollowing((long) listfollowings.size());
					
					Set<User> listfollowers = bu.getListfollowers();
					listfollowers.remove(b);
					bu.setFollowers((long)listfollowers.size());
				}
			}
			// no need of setters as it will update the lists automatically
//			bu.setBlockedByUsers(blockedByUsersSet);
//			b.setBlockedUsers(blockedUsersSet);
			userDao.save(b);
			userDao.save(bu);
			List<User> users = new ArrayList<>(List.of(b, bu));
			return users.stream().map(u -> UserResponse.convertUserResponse(u)).toList();
		}else {
			throw new ResourceNotFoundException("Either Blocker or BlockedUser is not present!");
		}
		
	}

}
