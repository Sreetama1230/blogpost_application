# 📝 Blogging Platform Application

A full-stack backend blogging system built using Spring Boot, supporting REST APIs, GraphQL APIs, and Kafka-based communication.

---

## 🚀 Features

* User Management with Role-Based Access (ADMIN, EDITOR, USER)
* Blog Post Creation & Management
* Category Management
* Comment System
* Like/Dislike System
* Follow/Unfollow & Block Users
* GraphQL APIs for advanced querying
* Kafka integration for inter-service communication
* JWT-based Authentication

---

## 🏗️ Tech Stack

* Java + Spring Boot
* Spring Security + JWT
* Spring Data JPA
* MySQL
* Kafka
* GraphQL
* Swagger (API Documentation)
* JUnit (Testing)

---

## 🗄️ Database Setup

```sql
CREATE DATABASE blog;
USE blog;
```

Tables:

* blog_post
* category
* comment
* liked_posts
* disliked_posts
* post_categories
* user
* user_blocked_users
* user_following
* user_roles

---

## 📚 API Documentation

Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 🔐 Authentication

* Login API returns JWT token
* Token validity: **1 hour**
* Required for secured endpoints

---

## 👤 User APIs

### Register User

`POST /users/register`

### Update User

`PUT /users`

### Get User by ID

`GET /users/{id}`

### Delete User

`DELETE /users/{id}`

Rules:

* ADMIN → full access
* EDITOR → limited
* USER → minimal
* Admin cannot delete another admin

---

## 📝 Blog APIs

### Create Blog

`POST /blogs`

### Update Blog

`PUT /blogs`

### Delete Blog

`DELETE /blogs/{id}`

### Fetch Blogs

* By user → `/users/{userId}/posts`
* By title → `/blogs/title/{title}/user/{userId}`

---

## 🏷️ Category APIs

* Get all → `GET /categories`
* Create → `POST /categories`
* Delete → `DELETE /categories/{id}`

Note:

* Only ADMIN can delete categories
* Category must not be linked to any blog

---

## 💬 Comment APIs

* Add → `POST /comments`
* Update → `PUT /comments`
* Get → `GET /comments/{id}`
* Delete → `DELETE /comments/{cId}/blogposts/{bpId}`

---

## 🔗 GraphQL APIs

Endpoint:

```
http://localhost:8080/graphql
```

Features:

* Pagination
* Search
* Trending Posts
* Follow/Unfollow Users
* Block Users
* Like/Dislike Posts
* Pinned Posts

---

## ⚡ Kafka Integration

* Used for communication between:

  * Blogging Platform
  * AdminTool

⚠️ Kafka is optional for basic APIs

Required for:

* Kafka Controller (`/admintool`)

---

## 🧪 Testing

* JUnit test cases included
* Covers:

  * Controller Layer
  * Service Layer
  * Repository Layer

---

## ▶️ Running the Application

### Without Kafka

* Start Spring Boot app
* Ignore Kafka-related errors

### With Kafka

1. Start Zookeeper
2. Start Kafka
3. Run AdminTool
4. Run Blogging Platform

---

## ⚠️ Notes

* One role per user (currently supported)
* JWT required for secured APIs
* Deleting a blog removes associated comments

---

## 🙌 Contribution

Feel free to fork, improve, and contribute.

---

## 📌 Author

Developed as a full-feature backend system demonstrating:

* REST + GraphQL APIs
* Security
* Messaging
* Scalable design

---

## 📢 Final Thought

This project shows a complete backend system with real-world features including authentication, messaging, and flexible API design.


