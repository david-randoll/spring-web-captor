package com.davidrandoll.spring_web_captor.properties;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ConfigurationCondition;

public class IsIpAddressEnabled extends AllNestedConditions {
    public IsIpAddressEnabled() {
        super(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "web-captor.additional-details.ip-address", havingValue = "true", matchIfMissing = true)
    static class IsEnabled {
    }
}
