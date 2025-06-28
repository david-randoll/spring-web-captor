package com.davidrandoll.spring_web_captor.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "web-captor")
public class WebCaptorProperties {
    private boolean enabled = true;

    @NestedConfigurationProperty
    private EventDetails eventDetails = new EventDetails();

    @NestedConfigurationProperty
    private AdditionalDetails additionalDetails = new AdditionalDetails();

    @Data
    public static class AdditionalDetails {
        private boolean duration = true;
        private boolean ipAddress = true;
        private boolean userAgent = true;
    }

    @Data
    public static class EventDetails {
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
        private boolean includeEndpointExists = true;
    }
}