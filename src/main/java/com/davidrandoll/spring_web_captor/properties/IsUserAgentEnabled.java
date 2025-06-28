package com.davidrandoll.spring_web_captor.properties;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class IsUserAgentEnabled extends AllNestedConditions {
    public IsUserAgentEnabled() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "web-captor.additional-details.user-agent", havingValue = "true", matchIfMissing = true)
    static class IsEnabled {
    }
}
