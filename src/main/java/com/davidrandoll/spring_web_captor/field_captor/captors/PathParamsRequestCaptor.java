package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.Map;

public class PathParamsRequestCaptor implements IRequestFieldCaptor {
    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        Map<String, String> params = this.getPathParams(request);
        builder.pathParams(params);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPathParams(HttpServletRequest request) {
        var pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables instanceof Map<?, ?>) {
            return (Map<String, String>) pathVariables;
        }
        return Collections.emptyMap();
    }
}