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

## Custom Event Publisher

By default, Spring Web Captor uses Spring's `ApplicationEventPublisher` to publish web capture events. However, if you prefer not to use `ApplicationEventPublisher`, you can provide your own implementation of the `IWebCaptorEventPublisher` interface.

For example, you could publish events to a message broker such as RabbitMQ, Kafka, or any other system as needed.

### Example

```java
@Component
public class MyCustomEventPublisher implements IWebCaptorEventPublisher {
    @Override
    public void publishEvent(Object event) {
        // Example: publish to RabbitMQ, Kafka, or any other system
        System.out.println("Captured event: " + event);
    }
}
```

You can then configure your application to use this custom publisher when setting up the web captor.

## Extending Event Details

This library supports adding additional details to the request/response event using the `IHttpEventExtension` interface. You can implement this interface to enrich captured HTTP events with custom information.

For example, the provided `ClientDetailsHttpEventExtension` implementation uses this interface to capture the user's IP address and User-Agent:

```java
public class ClientDetailsHttpEventExtension implements IHttpEventExtension {
     @Override
    public Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        // return a map with the user ip and user agent here
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        // return a map with the user ip and user agent here
    }
}
```

To use your own extension, implement the `IHttpEventExtension` interface and register it as a Spring bean. This allows your additional details to be automatically included in the captured events.

## Retrieving Additional Details from Published Events

When an HTTP event is published, any additional details added by your `IHttpEventExtension` implementation will be available in the event's `additionalData` map. You can access them as follows:

```java
@EventListener
public void handleHttpEvent(HttpEvent event) {
    // Retrieve additional details by key from the additionalData map
    Object clientIp = event.getAdditionalData().get("userIp");
    Object userAgent = event.getAdditionalData().get("userAgent");
    // Process the details as needed
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
 
## Contributing

Contributions are welcome! If you have suggestions for improvements or bug fixes, feel free to open an issue or submit a pull request.

To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make your changes and include tests if applicable.
4. Open a pull request describing your changes.

Please ensure your code follows the existing style and passes any automated checks.

Thank you for helping improve this project!

## License

MIT License

---

**Author:** [david-randoll](https://github.com/david-randoll)
