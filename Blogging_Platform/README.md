# Blogging Platform



Core service of the [blogpost_application](../README.md) system. Owns users, blog posts, comments, categories, the social graph (follow/block), reactions,pin/unpin and the timeline/feed — via both REST and GraphQL. It is the only Kafka **producer** in the system and the only service backed by a database.



Runs on **`:8080`**.



---



## Architecture (this service)



```
                         Client
                           │
             ┌─────────────┼──────────────┐
             │ REST (/blog, /user, …)     │ GraphQL (/graphql)
             ▼                            ▼
       ┌─────────────────────────────────────────┐
       │     JwtAuthFilter + SecurityConfig      │   ← JWT auth, role checks (ADMIN/EDITOR/USER)
       └──────────────────────┬──────────────────┘
                              ▼
       ┌─────────────────────────────────────────────────┐
       │        Service layer (BlogPostService,          │
       │   UserService, CommentService, GraphQlService)  │
       └───────────┬─────────────────────┬───────────────┘
                   │                     │
       POST /moderate (sync,           writes + outbox row
       retry ×3 / circuit-breaker)      (Event: PENDING)
                    ▼                     ▼
       ┌─────────────────────┐   ┌───────────────────────┐
       │ AIContentModeration-│   │  MySQL: blogposts_db  │
       │   Service (:8089)   │   └──────────┬────────────┘
       └─────────────────────┘              │
                                   polls PENDING every 5s
                                             ▼
                                 ┌───────────────────────┐
                                 │   EventPublisher      │
                                 │    (@Scheduled)       │
                                 └──────────┬────────────┘
                                            │ publishes to both
                                            ▼
                                 ┌────────────────────────────────┐
                                 │  Kafka: admin-topic            │
                                 │         notification-topic     │
                                 └────────────────────────────────┘
```



A second, separate path also writes to Kafka: `GET /admintool` sends the current logged-in username directly to `admin-topic`, bypassing the outbox above.



This service does not consume from Kafka — it only produces. AdminTool and NotificationService are the consumers; see the [root README](../README.md) for the full cross-service picture.



---



## Responsibilities



* User Management (create/update/delete, role assignment)
* Authentication & Authorization (JWT, RBAC)
* Blog Post Management (create/update/delete, sparse updates)
* Categories (create/delete, auto-created on blog post creation)
* Comments (create/update/delete/react)
* Followers / Following, Block / Unblock
* Pin / Unpin a post
* Reactions (like/dislike on posts, like/love/funny on comments)
* Timeline / Feed (personalized for logged-in users, popularity-ranked for guests)
* Publishing domain events to Kafka via the outbox pattern



---



## Auth



* JWT-based, stateless sessions, CSRF disabled
* Roles: `ADMIN > EDITOR > USER`, enforced via `@PreAuthorize` on GraphQL mutations/queries and path rules in `SecurityConfig` for REST
* `/login`, `/actuator/**`, `/admintool/**`, `/graphql/**`, `/graphiql/**`, `/timeline`, and GET on `/blog`, `/user`, `/category`, `/comment` are public; write operations require EDITOR/ADMIN/USER depending on resource



---



## Content moderation (outbound call)



Every blog post create/update calls `AIContentModerationService` synchronously (`POST http://aicontentmoder-service:8089/moderate`) **before** the post is saved:



* Wrapped in **Resilience4j** `@Retry` (3 attempts, 2s wait) and `@CircuitBreaker` (opens at ≥50% failure rate over a 10-call sliding window, half-opens after 20s)
* If Gemini returns HTTP 429 and the circuit trips, falls back to a local keyword blacklist instead of failing the request
* Rejected content raises `HarmfulContentException` — the post is never persisted



---



## Outbox pattern



Rather than publishing to Kafka inline with the DB write, every state change that should notify other services (`BlogPost`, `User`, `Category`, `Comment`, reactions, follow/block) first writes a row to the `Event` table (`status=PENDING`) in the same transaction as the domain write. A separate `@Scheduled` `EventPublisher` polls for `PENDING` rows every 5 seconds and publishes each to both `admin-topic` and `notification-topic`, retrying up to 10 times on failure (`PENDING → PROCESSING → PUBLISHED/FAILED`).



This means a domain write always succeeds or fails atomically with the DB, independent of whether Kafka is reachable at that instant.



---



## Idempotency & concurrency



* **Idempotency**: pass `requestId` as a request parameter (e.g. `POST /comment?blogPostId=1&requestId=67847`) — repeated calls with the same `requestId` return the original result regardless of body, tracked via the `ServiceRequestId` table.
* **Optimistic locking**: every entity has a `syncToken` field; updating with a stale token throws `StaleObjectError`.



---


