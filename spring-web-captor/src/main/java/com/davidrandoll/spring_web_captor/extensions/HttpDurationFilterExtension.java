package com.davidrandoll.spring_web_captor.extensions;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class HttpDurationFilterExtension extends OncePerRequestFilter implements IHttpEventExtension {
    private static final String START_TIME_KEY = "startTime";
    private static final String END_TIME_KEY = "endTime";
    private static final String DURATION_KEY = "duration";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        filterChain.doFilter(request, response);
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        Object startTimeObj = req.getAttribute(START_TIME_KEY);
        if (!(startTimeObj instanceof Long startTime)) return Map.of();
        var endTime = System.currentTimeMillis();

        Duration duration = Duration.ofMillis(endTime - startTime);
        return Map.of(
                DURATION_KEY, duration,
                START_TIME_KEY, startTime,
                END_TIME_KEY, endTime
        );
    }
}