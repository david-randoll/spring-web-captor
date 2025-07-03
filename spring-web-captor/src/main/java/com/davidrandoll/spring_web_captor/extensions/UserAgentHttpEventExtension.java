package com.davidrandoll.spring_web_captor.extensions;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * This class is responsible for extending HTTP events with user agent information.
 * It implements the IHttpEventExtension interface to provide additional information for both request and response events.
 */
@Slf4j
@RequiredArgsConstructor
public class UserAgentHttpEventExtension implements IHttpEventExtension {
    private static final String USER_AGENT = "userAgent";

    @Override
    public Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        return getUserAgent(req);
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        return getUserAgent(req);
    }

    private Map<String, Object> getUserAgent(HttpServletRequest request) {
        try {
            var userAgent = Optional.ofNullable(request.getHeader("User-Agent"))
                    .orElse("Unknown");
            return Map.of(USER_AGENT, userAgent);
        } catch (Exception e) {
            log.error("Error getting client details", e);
            return Map.of();
        }
    }
}
