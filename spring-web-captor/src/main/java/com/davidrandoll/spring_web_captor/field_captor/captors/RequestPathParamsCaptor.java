package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestPathParamsCaptor implements IRequestFieldCaptor {

    private static final Pattern CATCH_ALL_PATTERN = Pattern.compile("\\{\\*(.+?)}");

    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        Map<String, String> params = this.getPathParams(request);
        builder.pathParams(params);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPathParams(HttpServletRequest request) {
        var pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Map<String, String> params;
        if (pathVariables instanceof Map<?, ?>) {
            params = new LinkedHashMap<>((Map<String, String>) pathVariables);
        } else {
            params = new LinkedHashMap<>();
        }

        var pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String patternStr) {
            if (patternStr.contains("**")) {
                extractWildcardSegments(request, patternStr, params);
            } else {
                extractCatchAllSegments(patternStr, params);
            }
        }

        return params.isEmpty() ? Collections.emptyMap() : params;
    }

    /**
     * Handles ** wildcard patterns (e.g., {key}/**).
     * Extracts the unnamed segments matched by ** into path1, path2, etc.
     */
    private void extractWildcardSegments(HttpServletRequest request, String pattern, Map<String, String> params) {
        var pathWithinMapping = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String requestPath = pathWithinMapping instanceof String s ? s : request.getRequestURI();

        // Resolve named variables in the pattern
        String resolvedPattern = pattern;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            resolvedPattern = resolvedPattern.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // Split the resolved pattern by ** to get the literal anchor parts
        String[] parts = resolvedPattern.split("\\*\\*", -1);

        String remaining = requestPath;
        int pathIndex = 1;

        for (int i = 0; i < parts.length; i++) {
            String literalPart = parts[i];

            if (i == 0) {
                if (remaining.startsWith(literalPart)) {
                    remaining = remaining.substring(literalPart.length());
                }
            } else {
                if (literalPart.isEmpty()) {
                    pathIndex = addSegments(remaining, params, pathIndex);
                    remaining = "";
                } else {
                    int anchorIndex = remaining.indexOf(literalPart);
                    if (anchorIndex >= 0) {
                        String wildcardContent = remaining.substring(0, anchorIndex);
                        pathIndex = addSegments(wildcardContent, params, pathIndex);
                        remaining = remaining.substring(anchorIndex + literalPart.length());
                    }
                }
            }
        }
    }

    /**
     * Handles {*varName} catch-all path variables (e.g., {key}/{*rest}).
     * Spring stores the value as a single string like "/a/b/c".
     * This splits it into path1, path2, path3, etc. and removes the original catch-all key.
     */
    private void extractCatchAllSegments(String pattern, Map<String, String> params) {
        Matcher matcher = CATCH_ALL_PATTERN.matcher(pattern);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = params.remove(varName);
            if (value != null && !value.isEmpty()) {
                addSegments(value, params, 1);
            }
        }
    }

    private int addSegments(String path, Map<String, String> params, int startIndex) {
        if (path.isEmpty()) return startIndex;
        String[] segments = path.split("/");
        int index = startIndex;
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                params.put("path" + index, segment);
                index++;
            }
        }
        return index;
    }
}
