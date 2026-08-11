# BlogPost Application — Event-Driven Blogging Platform

## Overview

BlogPost Application is a Spring Boot based blogging platform that enables users to create and manage blog posts, comments, categories, followers, reactions, and user relationships.

The project follows an **event-driven architecture** using **Apache Kafka**, with content moderation handled synchronously via **Google Gemini** before a post is ever saved. The system is composed of **four independently deployable Spring Boot services**, each with its own port and Docker build context, wired together over REST and Kafka:

* **Blogging_Platform** (`:8080`) — the core platform: auth, posts, comments, categories, social graph, feed/timeline, GraphQL. The only Kafka producer in the system.
* **AIContentModerationService** (`:8089`) — stateless moderation gate, called synchronously by Blogging_Platform before a post is persisted.
* **AdminTool** (`:8081`) — Kafka consumer, audit/monitoring endpoint.
* **NotificationService** (`:8088`) — Kafka consumer, translates events into human-readable notification strings.

This project uses the **outbox pattern** for publishing events: writes are persisted to an `Event` table first, and a scheduled poller drains them onto Kafka — so an event is never lost even if Kafka is briefly unavailable.

Logging is implemented using **AOP and SLF4J**. Spring Boot **Actuator** is used to monitor, manage, and audit running applications, and its `/actuator/health` endpoint gates service startup ordering in Docker Compose.

---



## Architecture



```
                       ┌────────────┐
                       │   Client   │
                       └─────┬──────┘
                             │ POST /blog (JWT)
                             ▼
                   ┌───────────────────────┐        POST /moderate        ┌──────────────────────────────┐
                   │   Blogging_Platform   │ ───────────────────────────▶ │  AIContentModerationService  │
                   │        :8080          │   retry ×3 / circuit-breaker │            :8089             │
                   └──────────┬────────────┘ ◀─────────────────────────── └───────────────┬──────────────┘
                              │                                                           │
                   writes row +                                                  generateContent
                   Event(PENDING)                                                         ▼
                             ▼                                                 ┌─────────────────────┐
                   ┌──────────────────────┐                                    │  Google Gemini API  │
                   │  MySQL: blogposts_db │                                    │      (external)     │
                   └──────────┬───────────┘                                    └───────────┬─────────┘
                              │                                                            │
                   polls PENDING every 5s                                    on 429 → local keyword
                              ▼                                                blacklist fallback
                   ┌───────────────────┐
                   │   EventPublisher  │
                   │    (@Scheduled)   │
                   └──────────┬────────┘
                              │ publish to both topics
                              ▼
                   ┌───────────────────────────────────────┐
                   │               Kafka                   │
                   │  admin-topic  ·  notification-topic   │
                   └───────┬─────────────────────┬─────────┘
                           │ group-1             │ group-2
                           ▼                     ▼
                   ┌───────────────┐     ┌──────────────────────────┐
                   │   AdminTool   │     │   NotificationService    │
                   │     :8081     │     │         :8088            │
                   │  GET /events  │     │  GET /notification       │
                   │ (in-memory)   │     │    (in-memory)           │
                   └───────────────┘     └──────────────────────────┘
```



A second, separate producer path exists: `GET /admintool` on Blogging_Platform sends the logged-in username directly to `admin-topic`, bypassing the outbox/`Event` table entirely.

### Blogging Platform (core service, Kafka producer)
**REST** and **GraphQl** APIs are 
responsible for:

* User Management
* Authentication & Authorization
* Blog Post Management
* Categories
* Comments
* Followers / Following
* Blocking Users
* Pin/Unpin BlogPost
* Reactions
* Timeline/Feed

Publishes events to Kafka (via the outbox) when:

* User Created/Updated/Deleted
* Blog Post Created/Updated/Deleted/Reacted 
* Category Created/Deleted
* Comment Created/Updated/Deleted/Reacted
* Follow/Unfollow related
* Block/Unblock related
* Reacting on posts/comments, Pin/Unpin

### AI Content Moderation Service (stateless, synchronous)

Called by Blogging_Platform on every blog post create/update, **before** the post is persisted. Has no database and no Kafka involvement — a pure REST wrapper around the Gemini API.

Github Link - https://github.com/Sreetama1230/AIContentModeration

### Admin Tool (Kafka consumer)

Consumes events from `admin-topic` and the separate `/admintool` login-broadcast path) and exposes them via a **monitoring** endpoint.

State is held **in an in-memory list only** — nothing is persisted to a database, so the event list resets on service restart.

Example Response:
```json
[
    "transactionType=USER, transactionId=1, eventType=UPDATE, payload=\"Updated user while creating the blogpost: 1\", status=PROCESSING, createdAt=2026-08-10T12:14:06.504106, publishedAt=2026-08-10T12:14:06.505492, lastAttemptAt=2026-08-10T12:14:10.158964853, retryCount=0, recipientUserId=1, actorUserId=1",
    "transactionType=BLOGPOST, transactionId=4, eventType=CREATE, payload={\"id\":0,\"title\":\"cupoftea\",\"content\":\"started my day with a cup of tea\",\"categories\":[{\"name\":\"lifestyle\",\"syncToken\":null}],\"syncToken\":null}, status=PROCESSING, createdAt=2026-08-10T12:14:06.511552, publishedAt=2026-08-10T12:14:06.515968, lastAttemptAt=2026-08-10T12:14:10.435831164, retryCount=0, recipientUserId=1, actorUserId=1"
]
```

### Notification Service (Kafka consumer)

Consumes events from `notification-topic` (compact event strings) and maps them to human-readable notification messages. State is also held in memory only — messages are not persisted to a database, so `GET /notification` reflects only what's been consumed since the service last started.

---

## Features

### Authentication & Authorization

* JWT Authentication
* Spring Security
* Role-Based Access Control (RBAC)

Supported Roles:

* ADMIN
* EDITOR
* USER

Permission Hierarchy:

```text
ADMIN > EDITOR > USER
```
---

### User Management

**REST APIs**

A user can have only one valid role, and the email must contain `@`.

* Create User
* Update User (sparse update is allowed)
  * You cannot change the registered email ID or the role.
  * username and email should be unique.
* Delete User
* Get Blog Posts
* Get posts of a user
* Get all users

If you don't have the required permission, the request will fail with a **403** error.

**GraphQL APIs**

* Get pinned posts of the user
* User can pin a post and unpin the same post
* Get all the followers
* Get all the followings
* Get the user's liked posts
* User can set a reaction on a blog post (like/dislike)
* Follow or unfollow a user
* Block a user
* Get all the blocked users

---

### Blog Management

**REST APIs**
* Create Blog Posts
* Update Blog Posts (sparse update is allowed)
* Delete Blog Posts
* Category-Based Organization
* Get by title and user ID

**GraphQL APIs**
* Trending Posts
* Search Blog Posts by a keyword
* Pin and unpin Posts
* Like/dislike a post
* Get posts with Pagination

---

### Category Management

* Create Categories
* Delete Categories (only when no blog posts are linked with the category)
* View Posts by Category name
* Automatic Category Creation During Blog Creation

---

### Comment System

* Add Comments
* Edit Comments (sparse update is allowed)
  * After editing, if there is an actual change in the comment message, an "(edited)" suffix will be added to the comment message.
  * Example: `"content": "Nice Content!(edited)",`
* Delete Comments
* React to Comments

Supported Reactions:

* LIKE
* LOVE
* FUNNY

---

## Feed / Timeline API

Returns a paginated feed of posts. Logged-in users get posts from people they follow (plus some outside posts), ranked by net reactions (likes − dislikes) then recency; guests (or users with no follows) get a popularity feed ranked by reaction count and recency instead.

---

### Social Features

**GraphQL APIs**
* Follow Users
* Unfollow Users
* Block/Unblock Users
* View Followers
* View Following



### Kafka Integration

Kafka is used for **one-way, asynchronous fan-out** from Blogging_Platform. Blogging_Platform is the only producer in the system; AdminTool and NotificationService only ever consume — no topic flows back toward Blogging_Platform.

Two topics are published to on every outbox event:

* `admin-topic` — full event dump, consumed by AdminTool (`groupId=group-1`)
* `notification-topic` — compact `"<TransactionType> <EventType> <recipientUserId> <actorUserId>"` string, consumed by NotificationService (`groupId=group-2`)

A separate, non-outbox path also publishes to `admin-topic`: `GET /admintool` sends the current **logged-in username** directly to Kafka, bypassing the `Event` table. 

Example events (from the outbox):

Endpoint : `\events`
```
[
    "transactionType=USER, transactionId=1, eventType=UPDATE, payload=\"Updated user while creating the blogpost: 1\", status=PROCESSING, createdAt=2026-08-10T12:14:06.504106, publishedAt=2026-08-10T12:14:06.505492, lastAttemptAt=2026-08-10T12:14:10.158964853, retryCount=0, recipientUserId=1, actorUserId=1",
    "transactionType=BLOGPOST, transactionId=4, eventType=CREATE, payload={\"id\":0,\"title\":\"cupoftea\",\"content\":\"started my day with a cup of tea\",\"categories\":[{\"name\":\"lifestyle\",\"syncToken\":null}],\"syncToken\":null}, status=PROCESSING, createdAt=2026-08-10T12:14:06.511552, publishedAt=2026-08-10T12:14:06.515968, lastAttemptAt=2026-08-10T12:14:10.435831164, retryCount=0, recipientUserId=1, actorUserId=1"
]
```
Endpoint : ```\notification```
```
[
    "Profile Updated!",
    "Your blog post has been added successfully!",
    "Someone has liked your post!",
    "Someone has started following you!"
]
```

### Toggling Feature
The following GraphQL mutation operations support toggling behavior:

* `setReaction`
* `pinUnpinPost`
* `followOrUnFollowAuthor`
* `blockUser`

For example, invoking the same mutation twice will reverse the previous action (e.g., liking then unliking, following then unfollowing, or pinning then unpinning).

<br>
For example, when you hit the endpoint for the first time, the reaction is added. If you hit the same endpoint again with the same request body, the reaction is removed.

Try to mimic the behavior of popular social media platforms like Instagram or Facebook. When you click the Like button once, the post is liked. Clicking the Like button again removes the like.

```
mutation SetReaction {
				  setReaction(
				     request : {
				      bpId: 9,
				      uId: 25,
                      syncToken:4,
				      reaction: true
				    }
				  ) {
				  	id
				    content
				    createAt
				    likes
				  }
				}

```

---

## Tech Stack

### Backend

* Java 17 (Blogging_Platform, AIContentModerationService, AdminTool, NotificationService)
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* Spring Kafka
* GraphQL
* Resilience4j (Retry & Circuit Breaker)

### Database

* MySQL — used by **Blogging_Platform**. AIContentModerationService, AdminTool, and NotificationService are stateless/in-memory and have no database dependency.

### Messaging

* Apache Kafka (KRaft mode, no Zookeeper)

### Documentation

* Swagger / OpenAPI

### Testing

* JUnit 5
* Mockito

### Build Tool

* Maven

### Containerization

* Docker
* Docker Compose

---

## Database Design

Core Entities (Blogging_Platform, `blogposts_db`):

* User
* BlogPost
* Category
* Comment
* UserRole
* Followers
* Following
* Blocked Users
* Post Reactions
* Events (outbox table)
* ServiceRequestId (idempotency key store)

AdminTool and NotificationService hold no persistent entities — both keep consumed messages in an in-memory list that resets on restart.

---

## Running the Project

### Prerequisites

* Java 17+
* Maven
* Docker
* Docker Compose
* A `GEMINI_API_KEY` environment variable (required by AIContentModerationService — no default/fallback key is bundled)

### Clone Repository

```bash
git clone <repository-url>
cd blogpost_application
```

### Start All Services

```bash
docker compose up --build
```

This starts:

* Blogging_Platform
* AIContentModerationService
* AdminTool
* NotificationService
* MySQL
* Apache Kafka

Note: Blogging_Platform's container waits for AIContentModerationService's `/actuator/health` check to pass before starting (declared in `docker-compose.yml`).

---

## API Documentation

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### GraphQL Endpoint

```text
http://localhost:8080/graphql
```

### Admin Tool Events Endpoint

**Swagger UI**

```text
http://localhost:8081/swagger-ui/index.html
```

**Events Endpoint**

```text
http://localhost:8081/events
```



## Sample Execution Flow

```text
Register User
      ↓
Login
      ↓
Create Blog Post
      ↓
AIContentModerationService checks content (Gemini)
      ↓
Assign Categories
      ↓
Add Comments
      ↓
React / Follow
      ↓
Event row written (outbox, PENDING)
      ↓
EventPublisher polls every 5s → Kafka
      ↓
AdminTool consumes admin-topic  ·  NotificationService consumes notification-topic
```

---

## Testing

The project includes:

* Unit Tests
* Service Layer Tests
* Controller Layer Tests
* Security Tests

---
