# Spring Web Captor

A Spring Web library that intercepts and captures HTTP request/response data-including URI, path, query parameters, headers, and request/response body (including files) and publishes an event with this data.

## Features

- **Automatic HTTP interception:** Captures all incoming HTTP requests and outgoing responses in your Spring Boot application.
- **Rich event data:** Publishes events containing details such as method, path, query/path parameters, headers, and full request/response.
- **Multipart & file support:** Handles multipart/form-data and file uploads.
- **Content type handling:** Supports JSON, XML, plain text, form data, and more.
- **Spring Boot integration:** Provides auto-configuration for easy setup in Spring Boot projects.

## Usage

1. **Add to your Spring Boot project:**
   
   \*comming soon\* (Publish to Maven Central or GitHub Packages and add dependency instructions here.)

2. **Listen for HTTP events:**

   Implement an event listener for `HttpRequestEvent` and `HttpResponseEvent` to handle captured traffic.

   ```java
   @EventListener
   public void handleRequest(HttpRequestEvent event) {
       // Access event.getMethod(), event.getPath(), event.getRequestBody(), etc.
   }
   ```
   or for response body
   ```java
   @EventListener
   public void handleRequest(HttpResponseEvent event) {
       // Access event.getMethod(), event.getPath(), event.getResponseBody(), etc.
   }
   ```

## Captured Data

- **Request:**
  - Method (GET, POST, PUT, PATCH, DELETE etc.)
  - Full URL and Path
  - Query parameters
  - Path Variables
  - Headers (including multiple values)
  - Body (JSON, XML, form-data, files, etc.)

- **Response:**
  - All fields from the request event
  - Status code
  - Headers
  - Response Body

## License

MIT License

---

**Author:** [david-randoll](https://github.com/david-randoll)
