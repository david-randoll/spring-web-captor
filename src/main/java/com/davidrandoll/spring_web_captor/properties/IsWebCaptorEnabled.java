package com.davidrandoll.spring_web_captor.properties;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class IsWebCaptorEnabled extends AllNestedConditions {
    public IsWebCaptorEnabled() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "web-captor.enabled", havingValue = "true", matchIfMissing = true)
    static class IsEnabled {
    }
}
