package com.davidrandoll.spring_web_captor.publish_conditions;

import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ExcludedPathPublishCondition implements IHttpRequestPublishCondition, IHttpResponsePublishCondition {
    private final WebCaptorProperties properties;
    private final AntPathMatcher matcher;

    @Override
    public boolean shouldPublishRequest(HttpServletRequest request, HttpServletResponse response) {
        return isExcluded(request.getMethod(), request.getRequestURI());
    }

    @Override
    public boolean shouldPublishResponse(HttpServletRequest request, HttpServletResponse response) {
        return isExcluded(request.getMethod(), request.getRequestURI());
    }

    private boolean isExcluded(String method, String path) {
        HttpMethodEnum requestMethod = HttpMethodEnum.fromValue(method);

        return properties.getExcludedEndpoints().stream().noneMatch(rule ->
                toMethods(rule.getMethod()).contains(requestMethod) &&
                matcher.match(rule.getPath(), path)
        );
    }

    private List<HttpMethodEnum> toMethods(String method) {
        String cleaned = method == null ? "*" : method
                .replaceAll("\\s*", "")
                .replaceAll("^,|,$", "")
                .trim();

        if ("*".equalsIgnoreCase(cleaned)) {
            return List.of(HttpMethodEnum.values());
        }

        List<HttpMethodEnum> methods = new ArrayList<>();
        for (String part : cleaned.split(",")) {
            try {
                methods.add(HttpMethodEnum.fromValue(part));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid HTTP method '{}' in excluded request configuration. Skipping.", part);
            }
        }
        return methods;
    }
}
