package com.davidrandoll.spring_web_captor.properties;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class IsDurationEnabled extends AllNestedConditions {
    public IsDurationEnabled() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "web-captor.additional-details.duration", havingValue = "true", matchIfMissing = true)
    static class IsEnabled {
    }
}