package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import org.hibernate.annotations.ManyToAny;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Entity
public class User  implements Comparable<User>{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true, nullable = false)
	private String username;
	@Column(unique = true, nullable = false)
	private String password;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "role")
	private Set<String> roles = new HashSet<>();
	@Column
	private Long followers;
	
	@Column
	private Long following;
	
	@Column
	private String bio;

	@Column
	private String email;
	
	@Column
	private Long totalPosts;

	@OneToMany(mappedBy = "author")
	private List<BlogPost> blogPosts= new ArrayList<>(); 
	@OneToMany(mappedBy = "user")
	private List<Comment> comments= new ArrayList<>(); ;

	  // any particular user pinned some posts
	@OneToMany(mappedBy = "pinnedBy")
	private List<BlogPost> pinnedBlogPosts= new ArrayList<>(); 

	@ManyToMany
			
			@JoinTable (
					name="liked_posts",
					 joinColumns = @JoinColumn(name = "user_id"),
					 inverseJoinColumns = @JoinColumn (name = "post_id")
			)
	
	private List<BlogPost> likedBlogPosts= new ArrayList<>(); 

	
	@ManyToMany
	
	@JoinTable (
			name="disliked_posts",
			 joinColumns = @JoinColumn(name = "user_id"),
			 inverseJoinColumns = @JoinColumn (name = "post_id")
	)

private List<BlogPost> dislikedBlogPosts= new ArrayList<>(); 
	
	
	
	//  Users that this user follows
    @ManyToMany
    @JoinTable(
        name = "user_following",
        joinColumns = @JoinColumn(name = "follower_id"),         // this user
        inverseJoinColumns = @JoinColumn(name = "following_id")  // the user they follow
    )
    private Set<User> listfollowing = new HashSet<>();

    //  Users who follow this user
    @ManyToMany(mappedBy = "listfollowing")
    private Set<User> listfollowers = new HashSet<>();


    // Users this user has blocked
    @ManyToMany
    @JoinTable(
        name = "user_blocked_users",
        joinColumns = @JoinColumn(name = "blocker_id"),
        inverseJoinColumns = @JoinColumn(name = "blocked_id")
    )
    private Set<User> blockedUsers = new HashSet<>();

    // Users who have blocked this user
    @ManyToMany(mappedBy = "blockedUsers")
    private Set<User> blockedByUsers = new HashSet<>();
    
    
    
	public User(String username, String password, String email) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
	}
	public Long getId() {
		return id;
	}

	public Long getFollowers() {
		return followers;
	}
	public void setFollowers(Long followers) {
		this.followers = followers;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}
  
	public Set<User> getBlockedUsers() {
		return blockedUsers;
	}
	public void setBlockedUsers(Set<User> blockedUsers) {
		this.blockedUsers = blockedUsers;
	}
	public Set<User> getBlockedByUsers() {
		return blockedByUsers;
	}
	public void setBlockedByUsers(Set<User> blockedByUsers) {
		this.blockedByUsers = blockedByUsers;
	}
	public List<BlogPost> getLikedBlogPosts() {
		return likedBlogPosts;
	}
	
	
	public List<BlogPost> getDislikedBlogPosts() {
		return dislikedBlogPosts;
	}
	public void setDislikedBlogPosts(List<BlogPost> dislikedBlogPosts) {
		this.dislikedBlogPosts = dislikedBlogPosts;
	}
	public void setLikedBlogPosts(List<BlogPost> likedBlogPosts) {
		this.likedBlogPosts = likedBlogPosts;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}


	public Set<User> getListfollowing() {
		return listfollowing;
	}
	public void setListfollowing(Set<User> listfollowing) {
		this.listfollowing = listfollowing;
	}
	public Set<User> getListfollowers() {
		return listfollowers;
	}
	public void setListfollowers(Set<User> listfollowers) {
		this.listfollowers = listfollowers;
	}


	public List<BlogPost> getBlogPosts() {
		return blogPosts;
	}

	public void setBlogPosts(List<BlogPost> blogPosts) {
		this.blogPosts = blogPosts;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}


	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", password=" + password + ", email=" + email
				+ ", blogPosts=" + blogPosts + ", comments=" + comments + "]";
	}

	public User() {
		super();
	}

	public User(String username, String password, String email, ArrayList<BlogPost> blogPosts,
			ArrayList<Comment> comments) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
		this.blogPosts = blogPosts;
		this.comments = comments;
	}
	public Long getFollowing() {
		return following;
	}
	public void setFollowing(Long following) {
		this.following = following;
	}
	public String getBio() {
		return bio;
	}
	public void setBio(String bio) {
		this.bio = bio;
	}
	public Long getTotalPosts() {
		return totalPosts;
	}
	public void setTotalPosts(Long totalPosts) {
		this.totalPosts = totalPosts;
	}
	public List<BlogPost> getPinnedBlogPosts() {
		return pinnedBlogPosts;
	}
	public void setPinnedBlogPosts(List<BlogPost> pinnedBlogPosts) {
		this.pinnedBlogPosts = pinnedBlogPosts;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash( username);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		return Objects.equals(username, other.username);
	}
	@Override
	public int compareTo(User u) {
		return this.getUsername().compareTo(u.getUsername());
	}


}
