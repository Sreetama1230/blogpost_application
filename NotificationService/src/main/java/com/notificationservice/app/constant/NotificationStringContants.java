package com.notificationservice.app.constant;

public class NotificationStringContants {

	// thinking as a logged in user perspective 
	// thinking we will just trigger the notification for "positive actions" ignored actions such that dislike,unfollow
	// after the user has been created will start sending the notification
	public static String USER_UPDATED = "Profile Updated!";
	public static String USER_DELETED = "Profile Deleted!";
	public static String USER_FOLLOW = "Someone has started following you!";

	// not sending any notification for unfollow / block/ unblock
	
	
	public static String BlOGPOST_CREATED = "Your blog post has been added successfully!";
	public static String BlOGPOST_UPDATED = "Your blog post has been updated successfully!";
	public static String BLOGPOST_REACTED_LIKE = "Someone has liked your post!";
	public static String BLOGPOST_DELETED = "You blog post has been deleted successfully";
	public static String BLOGPOST_PIN = "Someone has pinned your post!";
	
	// not sending anything for un-pin
	public static String COMMENT_ADDED = "Someone has commented to your blog!";
	public static String COMMENT_REACTED = "Someone reacted to your comment";
	public static String COMMENT_UPDATED = "You have updated the comment successfully!";
	public static String COMMENT_DELETED = "Comment Deleted!";
	
	public static String CATEGORY_ADDED = "Category Added!";
	public static String CATEGORY_DELETED = "Category Deleted!";
	
	
}
