package com.davidrandoll.spring_web_captor.publisher.request;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class HttpRequestEventPublisher implements HandlerInterceptor {
    private final IWebCaptorEventPublisher publisher;
    private final List<IHttpEventExtension> httpEventExtensions;
    private final IBodyParserRegistry bodyParserRegistry;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request, bodyParserRegistry);
        requestWrapper.setEndpointExists(true);

        HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent();
        if (!requestEvent.getPath().equalsIgnoreCase("/error")) {
            requestWrapper.publishEvent(httpEventExtensions, publisher, response);
        }

        return true;
    }
}