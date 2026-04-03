# Spring Web Captor

[![Maven Central](https://img.shields.io/maven-central/v/com.davidrandoll/spring-web-captor)](https://central.sonatype.com/artifact/com.davidrandoll/spring-web-captor)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-green)

**Zero-config HTTP observability for Spring Boot.**

Add one dependency, write an event listener, and every HTTP request and response in your application is automatically captured as a structured event - complete with method, URL, headers, body (JSON, XML, multipart, form-data), status code, errors, and more.

**[Live Demo](https://captor.davidrandoll.com)** | **[Example App](examples/)**

---

## Why Spring Web Captor?

- **Zero boilerplate** - add the dependency, listen for events. No filters, interceptors, or wrapper classes to write.
- **Full HTTP fidelity** - captures method, URL, path/query params, headers, request body, response body, status code, errors, and file uploads.
- **Production-ready toggles** - enable/disable any captured field, exclude endpoints by path pattern, or conditionally publish events - all via `application.properties`.
- **Pluggable architecture** - swap the event publisher (Kafka, RabbitMQ), add custom body parsers, field captors, or event enrichers.
- **Spring Boot native** - auto-configured starter with `@ConditionalOnMissingBean` everywhere. Works with Spring Security and error handling out of the box.
- **Well-tested** - 340+ tests covering JSON, XML, multipart, concurrent requests, error scenarios, and more.

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.davidrandoll</groupId>
    <artifactId>spring-web-captor</artifactId>
    <version>1.0.3</version>
</dependency>
```

<details>
<summary>Gradle</summary>

```groovy
implementation 'com.davidrandoll:spring-web-captor:1.0.3'
```
</details>

### 2. Listen for events

```java
@Component
@Slf4j
public class HttpEventLogger {

    @EventListener
    public void onRequest(HttpRequestEvent event) {
        log.info(">> {} {}", event.getMethod(), event.getPath());
    }

    @EventListener
    public void onResponse(HttpResponseEvent event) {
        log.info("<< {} {} -> {}", event.getMethod(), event.getPath(), event.getResponseStatus());
    }
}
```

### 3. Run your app

Start your Spring Boot application. Every HTTP request is now captured and published as an event.

**That's it. No configuration required.**

---

## Configuration Reference

All properties are optional. The library works out of the box with sensible defaults.

### Core

| Property | Type | Default | Description |
|---|---|---|---|
| `web-captor.enabled` | `boolean` | `true` | Enable or disable all HTTP capturing |

### Event Details (`web-captor.event-details.*`)

Toggle individual fields captured in events:

| Property | Default | Description |
|---|---|---|
| `include-endpoint-called` | `true` | Whether a mapped controller endpoint was matched |
| `include-full-url` | `true` | Complete request URL (scheme + host + path + query) |
| `include-path` | `true` | Request path |
| `include-method` | `true` | HTTP method (GET, POST, etc.) |
| `include-request-headers` | `true` | Request headers |
| `include-query-params` | `true` | Query string parameters |
| `include-path-params` | `true` | Path variables (e.g. `/users/{id}`) |
| `include-request-body` | `true` | Parsed request body |
| `include-multipart-files` | `true` | Multipart file metadata |
| `include-response-headers` | `true` | Response headers |
| `include-response-body` | `true` | Parsed response body |
| `include-response-status` | `true` | HTTP status code |
| `include-error-details` | `true` | Exception details for failed requests |

### Additional Details (`web-captor.additional-details.*`)

Built-in enrichment fields:

| Property | Default | Description |
|---|---|---|
| `duration` | `true` | Request processing duration in milliseconds |
| `ip-address` | `true` | Client IP address |
| `user-agent` | `true` | User-Agent header value |

### Excluded Endpoints

Exclude specific paths and methods from event publishing using Ant-style patterns:

```yaml
web-captor:
  excluded-endpoints:
    - path: /actuator/**
      method: "*"          # all methods (default)
    - path: /api/health
      method: GET
    - path: /swagger-ui/**
      method: GET,POST
```

---

## Captured Data

| Request Event (`HttpRequestEvent`) | Response Event (`HttpResponseEvent`) |
|---|---|
| HTTP method (GET, POST, PUT, PATCH, DELETE, etc.) | All request event fields |
| Full URL and path | HTTP status code |
| Query parameters | Response headers |
| Path variables | Response body (JSON, XML, text) |
| Request headers (multi-value) | Error details (exception message, stack trace) |
| Request body (JSON, XML, form-data, multipart) | |
| File upload metadata | |
| Custom fields via extensions and field captors | |

---

## Advanced Usage

<details>
<summary><strong>Custom Event Publisher</strong></summary>

By default, events are published via Spring's `ApplicationEventPublisher`. To publish to Kafka, RabbitMQ, or any other system, implement `IWebCaptorEventPublisher`:

```java
@Component
public class KafkaEventPublisher implements IWebCaptorEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishEvent(Object event) {
        kafkaTemplate.send("http-events", event);
    }
}
```

Your bean automatically replaces the default publisher via `@ConditionalOnMissingBean`.

</details>

<details>
<summary><strong>Event Extensions</strong></summary>

Enrich events with custom data by implementing `IHttpEventExtension`. The returned map is merged into the event's `additionalData`:

```java
@Component
public class SecurityContextExtension implements IHttpEventExtension {
    @Override
    public Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return Map.of("userId", auth.getName());
        }
        return Map.of();
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        return Map.of(); // or enrich response events too
    }
}
```

Access enriched data from listeners:

```java
@EventListener
public void onRequest(HttpRequestEvent event) {
    Object userId = event.getAdditionalData().get("userId");
}
```

**Built-in extensions** (enabled by default via `web-captor.additional-details.*`):
- `HttpDurationFilterExtension` - request duration in ms
- `IpAddressHttpEventExtension` - client IP address
- `UserAgentHttpEventExtension` - User-Agent header

</details>

<details>
<summary><strong>Custom Field Captors</strong></summary>

Extract and transform individual fields before event publication using `IRequestFieldCaptor` or `IResponseFieldCaptor`:

```java
@Component
@Order(1)
public class CorrelationIdCaptor implements IRequestFieldCaptor {
    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null) {
            builder.additionalData("correlationId", correlationId);
        }
    }
}
```

Use `@Order` to control execution priority when multiple captors are registered.

</details>

<details>
<summary><strong>Custom Body Parsers</strong></summary>

Add support for additional content types by implementing `IRequestBodyParser` or `IResponseBodyParser`:

```java
@Component
@Order(5)
public class ProtobufRequestBodyParser implements IRequestBodyParser {
    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("application/protobuf");
    }

    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) throws IOException {
        // Parse protobuf bytes into a JsonNode
        return new BodyPayload(jsonNode);
    }
}
```

**Built-in parsers:** JSON, multipart/form-data, x-www-form-urlencoded, and plain text (fallback).

Use `@Order` to control parser priority. The first parser whose `supports()` returns `true` is used.

</details>

<details>
<summary><strong>Conditional Publishing</strong></summary>

Control when events are published by implementing `IHttpRequestPublishCondition` or `IHttpResponsePublishCondition`:

```java
@Component
public class AuthenticatedOnlyCondition implements IHttpRequestPublishCondition {
    @Override
    public boolean shouldPublishRequest(HttpServletRequest request, HttpServletResponse response) {
        return request.getUserPrincipal() != null;
    }
}
```

All registered conditions must return `true` for an event to be published (logical AND).

> **Tip:** For simple path/method exclusion, use the [`web-captor.excluded-endpoints`](#excluded-endpoints) property instead of writing code.

</details>

---

## XML Extension

For XML request/response body parsing, add the XML extension module:

```xml
<dependency>
    <groupId>com.davidrandoll</groupId>
    <artifactId>spring-web-captor-xml</artifactId>
    <version>1.0.3</version>
</dependency>
```

<details>
<summary>Gradle</summary>

```groovy
implementation 'com.davidrandoll:spring-web-captor-xml:1.0.3'
```
</details>

Auto-configured - just add the dependency. Requires `jackson-dataformat-xml` on your classpath.

---

## How It Works

Spring Web Captor hooks into the servlet layer using a `HandlerInterceptor` (for requests) and a `OncePerRequestFilter` (for responses):

1. **Wrap** - the request and response are wrapped to cache body bytes for non-destructive reading.
2. **Capture** - registered `IRequestFieldCaptor` / `IResponseFieldCaptor` beans extract fields (method, path, headers, body, etc.) into event builders.
3. **Parse** - `IBodyParserRegistry` selects the appropriate parser by content type and parses the body into a structured `BodyPayload`.
4. **Gate** - `IHttpRequestPublishCondition` / `IHttpResponsePublishCondition` beans determine whether to publish the event.
5. **Enrich** - `IHttpEventExtension` beans add custom data (duration, IP, user agent, or your own).
6. **Publish** - `IWebCaptorEventPublisher` publishes the final `HttpRequestEvent` / `HttpResponseEvent`.

Every component is replaceable - register your own Spring bean and the default is automatically swapped out.

---

## Contributing

Contributions are welcome! If you have suggestions or bug fixes, feel free to [open an issue](https://github.com/david-randoll/spring-web-captor/issues) or submit a pull request.

To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make your changes and include tests if applicable.
4. Open a pull request describing your changes.

**Requirements:** Java 21+, Maven.

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

---

**Author:** [david-randoll](https://github.com/david-randoll)
