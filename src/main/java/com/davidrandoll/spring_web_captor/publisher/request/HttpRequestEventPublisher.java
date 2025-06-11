package com.davidrandoll.spring_web_captor.publisher.request;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Slf4j
@Component("httpRequestEventPublisher")
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "httpRequestEventPublisher", ignored = HttpRequestEventPublisher.class)
public class HttpRequestEventPublisher implements HandlerInterceptor {
    private final IWebCaptorEventPublisher publisher;
    private final List<IHttpEventExtension> httpEventExtensions;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request, objectMapper);
        requestWrapper.setEndpointExists(true);

        HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent();
        publishRequestEvent(requestEvent);

        return true;
    }

    private void publishRequestEvent(HttpRequestEvent requestEvent) {
        if (requestEvent.getPath().equalsIgnoreCase("/error")) {
            return;
        }

        for (IHttpEventExtension extension : httpEventExtensions) {
            var additionalData = extension.extendRequestEvent(requestEvent);
            requestEvent.addAdditionalData(additionalData);
        }
        publisher.publishEvent(requestEvent);
    }
}