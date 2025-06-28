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
    private AdditionalDetails additionalDetails = new AdditionalDetails();

    @Data
    public static class AdditionalDetails {
        private boolean duration = true;
        private boolean ipAddress = true;
        private boolean userAgent = true;
    }
}