# Spring Web Captor

A Spring Web library that intercepts and captures HTTP request/response data-including URI, path, query parameters,
headers, and request/response body (including files) and publishes an event with this data.

## Features

- **Automatic HTTP interception:** Captures all incoming HTTP requests and outgoing responses in your Spring Boot
  application.
- **Rich event data:** Publishes events containing details such as method, path, query/path parameters, headers, and
  full request/response.
- **Multipart & file support:** Handles multipart/form-data and file uploads.
- **Content type handling:** Supports JSON, XML, plain text, form data, and more.
- **Spring Boot integration:** Provides auto-configuration for easy setup in Spring Boot projects.
- **Conditional HTTP Event Publishing:** Interfaces like `IHttpRequestPublishCondition` and
  `IHttpResponsePublishCondition` allow flexible, condition-based publishing of HTTP events.
- **Excluded Endpoints Support:** Easily configure HTTP methods and paths to exclude from event publishing.
- **Extensible Publisher & Parsing:** Implement your own `IHttpEventPublisher`, `IRequestBodyParser`,
  `IResponseBodyParser`, `IRequestFieldCaptor`, or `IResponseFieldCaptor` for custom event publication and field/body
  parsing.

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

By default, Spring Web Captor uses Spring's `ApplicationEventPublisher` to publish web capture events. However, if you
prefer not to use `ApplicationEventPublisher`, you can provide your own implementation of the `IWebCaptorEventPublisher`
interface.

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

This library supports adding additional details to the request/response event using the `IHttpEventExtension` interface.
You can implement this interface to enrich captured HTTP events with custom information.

For example, the provided `ClientDetailsHttpEventExtension` implementation uses this interface to capture the user's IP
address and User-Agent:

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

To use your own extension, implement the `IHttpEventExtension` interface and register it as a Spring bean. This allows
your additional details to be automatically included in the captured events.

## Retrieving Additional Details from Published Events

When an HTTP event is published, any additional details added by your `IHttpEventExtension` implementation will be
available in the event's `additionalData` map. You can access them as follows:

```java

@EventListener
public void handleHttpEvent(HttpEvent event) {
    // Retrieve additional details by key from the additionalData map
    Object clientIp = event.getAdditionalData().get("userIp");
    Object userAgent = event.getAdditionalData().get("userAgent");
    // Process the details as needed
}
```

## Customizing Request/Response Body Parsing

Spring Web Captor allows you to customize how HTTP bodies are parsed by implementing the `IRequestBodyParser` or
`IResponseBodyParser` interface. This is useful if you want to support additional content types or handle
request/response bodies in a specific way.

### Example: Custom JSON Request Body Parser

The library provides a built-in example for parsing JSON request bodies. You can implement your own parser by following
a similar pattern:

```java

@RequiredArgsConstructor
@Order(1)
public class JsonRequestBodyParser implements IRequestBodyParser {
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("json");
    }

    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) throws IOException {
        if (ObjectUtils.isEmpty(body)) {
            return new BodyPayload(JsonNodeFactory.instance.nullNode());
        }
        return new BodyPayload(objectMapper.readTree(body));
    }
}
```

```java

@RequiredArgsConstructor
@Order(1)
public class JsonResponseBodyParser implements IResponseBodyParser {
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("json");
    }

    @Override
    public BodyPayload parse(HttpServletResponse response, byte[] body) throws IOException {
        if (ObjectUtils.isEmpty(body)) {
            return new BodyPayload(JsonNodeFactory.instance.nullNode());
        }
        return new BodyPayload(objectMapper.readTree(body));
    }
}
```

**How it works:**

- The `supports` method checks if the `Content-Type` header includes `"json"`.
- If supported, the `parse` method uses Jackson's `ObjectMapper` to deserialize the request body into a `JsonNode`,
  which is then wrapped in a `BodyPayload` object.

To use your own parser, implement `IRequestBodyParser` or `IResponseBodyParser`, and register it via the
`IBodyParserRegistry.register` or injects the `DefaultBodyParserRegistry` bean and call the `register` method in your
configuration class.

The registry is there so that you can use the built-in parsers or your own custom parsers seamlessly.

> **Tip:** You can provide multiple parsers for different content types (e.g., XML, protobuf, etc.) and control their
> order with the `@Order` annotation.
> By default, the library includes request body parsers for JSON, XML, multipart,
> x-www-form-urlencoded and a fallback parser for plain text.

## Customizing Field Capture: IRequestFieldCaptor and IResponseFieldCaptor

Spring Web Captor gives you fine-grained control over which fields are captured from requests and responses by allowing
you to implement the `IRequestFieldCaptor` and `IResponseFieldCaptor` interfaces. These interfaces let you add,
transform, or filter specific fields before they are published in the event.

### Example: Capturing a Custom Request Header

Suppose you want to extract a custom header ("X-Request-Token") from every incoming request and include it in the
captured event. You can implement `IRequestFieldCaptor` as follows:

```java
import com.davidrandoll.spring_web_captor.capture.IRequestFieldCaptor;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
@Order(1)
public class XRequestTokenFieldCaptor implements IRequestFieldCaptor {
    @Override
    public void captureFields(HttpServletRequest servletRequest, HttpRequestEvent event) {
        String token = servletRequest.getHeader("X-Request-Token");
        if (token != null) {
            event.addAdditionalData("xRequestToken", token);
        }
    }
}
```

### Example: Capturing a Custom Response Header

Likewise, you can capture fields from the response by implementing `IResponseFieldCaptor`. For example, to capture an "
X-Response-Token" header:

```java
import com.davidrandoll.spring_web_captor.capture.IResponseFieldCaptor;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
@Order(1)
public class XResponseTokenFieldCaptor implements IResponseFieldCaptor {
    @Override
    public void captureFields(HttpServletResponse servletResponse, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        String token = servletResponse.getHeader("X-Response-Token");
        if (token != null) {
            resEvent.addAdditionalData("xResponseToken", token);
        }
    }
}
```

**How it works:**

- Implement the appropriate interface and annotate your class with `@Component` (and optionally `@Order`).
- Use the `captureFields` method to extract, transform, or add any extra information to the event’s `additionalData`
  map.

> **Tip:** You can implement multiple captors and control their order of execution with the `@Order` annotation.

## Conditional HTTP Event Publishing

Spring Web Captor allows you to control exactly when HTTP request and response events are published by implementing
conditional interfaces. This enables you to filter out specific requests or responses based on any logic you need (e.g.,
only publish events for authenticated users, or requests to certain paths).

There are two main interfaces for this purpose:

- `IHttpRequestPublishCondition`
- `IHttpResponsePublishCondition`

Implement either (or both) to define custom logic for event publishing.

### Example: Only Publish Events for Authenticated Requests

Here's how you might implement a condition to publish events only if the user is authenticated:

```java
@Component
@Order(1)
public class AuthenticatedRequestPublishCondition implements IHttpRequestPublishCondition {
    @Override
    public boolean shouldPublishRequest(HttpServletRequest request, HttpServletResponse response) {
        // Example: Only publish if a user principal is present (i.e., the user is authenticated)
        return request.getUserPrincipal() != null;
    }
}
```

You can register multiple condition beans. All conditions must return `true` for the event to be published (logical
AND).

> **Note:** If you want to exclude requests to specific endpoints (such as health checks or documentation), you can do
> this easily using the built-in app property `web-captor.excluded-endpoints` in your configuration, without
> needing custom code.

**Summary:**

- Use `IHttpRequestPublishCondition` and/or `IHttpResponsePublishCondition` for programmatic, logic-based control.
- Use the `excluded-endpoints` property for simple endpoint/path/method exclusion.

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

Contributions are welcome! If you have suggestions for improvements or bug fixes, feel free to open an issue or submit a
pull request.

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
