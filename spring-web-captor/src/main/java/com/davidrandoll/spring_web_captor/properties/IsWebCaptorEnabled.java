package com.davidrandoll.spring_web_captor.properties;

import com.davidrandoll.spring_web_captor.utils.ConditionalUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class IsWebCaptorEnabled implements Condition {
    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        return ConditionalUtils.evaluate(context, "web-captor.enabled", "true", true);
    }
}