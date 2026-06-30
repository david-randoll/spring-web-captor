package com.davidrandoll.spring_web_captor.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Data
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "web-captor")
public class WebCaptorProperties {
    private boolean enabled = true;

    @NestedConfigurationProperty
    private EventDetails eventDetails = new EventDetails();

    @NestedConfigurationProperty
    private AdditionalDetails additionalDetails = new AdditionalDetails();

    private List<ExcludedRequest> excludedEndpoints = new ArrayList<>();

    /**
     * Package-name prefixes whose exceptions should be re-thrown from
     * {@code UnhandledExceptionResponseFilter} instead of being rendered as a 500. Used when an
     * outer servlet filter (one that runs <em>outside</em> our captor filters in the chain) is
     * expected to translate the exception itself — e.g. Spring Security's
     * {@code ExceptionTranslationFilter} calling {@code sendError(403)} for
     * {@code AccessDeniedException}.
     *
     * <p>The default list covers Spring Security. To extend for another framework with the same
     * translate-in-an-outer-filter pattern, replace or augment the list via configuration:
     *
     * <pre>{@code
     * web-captor:
     *   defer-outer-filter-packages:
     *     - org.springframework.security.
     *     - com.acme.security.
     * }</pre>
     *
     * <p>The captor walks the exception's cause chain (depth-limited, self-reference-safe) and
     * defers if any frame's class name starts with one of these prefixes. Subclasses are matched
     * automatically — no per-class list to maintain.
     */
    private List<String> deferOuterFilterPackages = new ArrayList<>(List.of("org.springframework.security."));

    @Data
    public static class AdditionalDetails {
        private boolean duration = true;
        private boolean ipAddress = true;
        private boolean userAgent = true;
    }

    @Data
    public static class EventDetails {
        private boolean includeEndpointCalled = true;
        private boolean includeFullUrl = true;
        private boolean includePath = true;
        private boolean includeMethod = true;
        private boolean includeRequestHeaders = true;
        private boolean includeQueryParams = true;
        private boolean includePathParams = true;
        private boolean includeRequestBody = true;
        private boolean includeMultipartFiles = true;
        private boolean includeResponseHeaders = true;
        private boolean includeResponseBody = true;
        private boolean includeResponseStatus = true;
        private boolean includeErrorDetails = true;
    }

    @Data
    public static class ExcludedRequest {
        private String method = "*"; // Default to all methods
        private String path;
    }
}