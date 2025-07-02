package com.example.demo.gqlcontroller;

import java.util.List;

import com.example.demo.exception.FollowUnFollowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.example.demo.dto.ReactDTO;
import com.example.demo.gqlservice.PostsService;
import com.example.demo.response.BlogPostResponse;
import com.example.demo.response.PinnedBlogPost;
import com.example.demo.response.UserResponse;

@Controller
public class PostsController {

	@Autowired
	private PostsService postservice;

	
	Logger logger = LoggerFactory.getLogger(PostsController.class);
	@QueryMapping("searchPosts")
	public  List<BlogPostResponse> searchPosts( @Argument String keyword){
		
		try {
			return postservice.searchPosts(keyword);
		} catch (Exception e) {
		  e.printStackTrace();
		}
		return null;
	}
	
	@QueryMapping("getPosts")
	public Page<BlogPostResponse> getPosts( @Argument int page, @Argument int size){
		
	   return postservice.getPosts(page, size);
	}
	
	@QueryMapping("getPinnedPostsOfTheUser")
	@PreAuthorize("#uId == authentication.principal.id")
	public List<PinnedBlogPost> getPinnedPostsOfTheUser(@Argument("uId") long  uId){
		return postservice.getPinnedPostsOfTheUser(uId);
	}
	
	@QueryMapping("getFollowers")
	@PreAuthorize("#uId == authentication.principal.id")
	public  List<UserResponse>   getFollowers( @Argument  long uId) {
		return postservice.getFollowers(uId);
		
	}
	
	@QueryMapping("getFollowings")
	@PreAuthorize("#uId == authentication.principal.id")
	public  List<UserResponse>   getFollowings( @Argument long uId) {
		return postservice.getFollowings(uId);
	}
	
	@QueryMapping("trendingPosts")
	public Page<BlogPostResponse>   trendingPosts() {
		return postservice.trendingPosts();
	}

	@QueryMapping("userLikedPost")
	@PreAuthorize("#uId == authentication.principal.id")
	public List<BlogPostResponse> userLikedPost( @Argument  long uId){
		return postservice.userLikedPost(uId);
	}
	
	@QueryMapping("blockedUsers")
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR') and #uId == authentication.principal.id")
	public List<UserResponse> blockedUsers( @Argument long uId) {
		return postservice.getBlockedUsers(uId);
	}

	@MutationMapping("setReaction")
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'USER')")
	public BlogPostResponse setReaction(  @Argument ReactDTO request) {
		
		return postservice.setReaction(request);
	}
	
	@MutationMapping("pinnedPost")
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'USER') and #uId == authentication.principal.id")
	public PinnedBlogPost pinnedPost(@Argument   long uId  , @Argument  long bpId) {
		return postservice.pinnedPost(uId, bpId);
	}
	
	@MutationMapping("followOrUnFollowAuthor")
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'USER') and #follower == authentication.principal.id")
	public  List<UserResponse> followOrUnFollowAuthor(  @Argument long follower  , @Argument long followee ) {
	
		try {
			return postservice.followOrUnFollowAuthor(follower, followee);
		}catch(FollowUnFollowException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
	//Always validate user ID arguments against the authenticated user’s ID.
	@MutationMapping("blockUser")
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')  and #blockerId == authentication.principal.id")
	public List<UserResponse> blockUser(  @Argument("blockerId") Long blockerId, @Argument Long blockedUserId)
	{
		if (blockerId == null || blockedUserId == null) {
			throw new IllegalArgumentException("blockerId and blockedUserId must not be null");
		}
		return postservice.blockUser(blockerId, blockedUserId);
	}
	
	
}
