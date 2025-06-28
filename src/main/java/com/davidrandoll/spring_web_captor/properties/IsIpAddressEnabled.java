package com.davidrandoll.spring_web_captor.properties;

import com.davidrandoll.spring_web_captor.utils.ConditionalUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class IsIpAddressEnabled implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return ConditionalUtils.evaluate(context, "web-captor.additional-details.ip-address", "true", true);
    }
}
