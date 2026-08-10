

## Content Moderation

The AI Content Moderation Service is a dedicated Spring Boot microservice responsible for analyzing blog content before publication. It communicates with the BlogPost service through REST APIs and uses the Google Gemini API to determine whether submitted content complies with predefined safety guidelines.

 GitHub Link- [BlogPostApplication](https://github.com/Sreetama1230/blogpost_application)

 ```
          REST API
+------------------------------+
|      BlogPost Service        |
+------------------------------+
              |
              | HTTP
              v
+------------------------------+
| Content Moderation Service   |
+------------------------------+
              |
              | HTTPS
              v
      Google Gemini API

```
### How it Works

1. The BlogPost service sends the blog title, content, and categories to the AI Content Moderation Service.
2. The moderation service uses the **Google Gemini API** to evaluate the content against predefined safety guidelines.
3. Based on the AI response, the service either:

   * **Approves** the content for publishing, or
   * **Rejects** the content if it contains harmful, violent, abusive, or illegal material.

### Technologies Used

* Java 17
* Spring Boot
* Google Gemini API
* REST APIs

