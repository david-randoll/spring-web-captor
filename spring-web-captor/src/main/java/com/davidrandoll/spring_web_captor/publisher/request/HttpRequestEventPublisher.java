package com.davidrandoll.spring_web_captor.publisher.request;

import com.davidrandoll.spring_web_captor.publisher.IHttpEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class HttpRequestEventPublisher implements HandlerInterceptor {
    private final IHttpEventPublisher publisher;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper);
        requestWrapper.setEndpointExists(true);

        if (!requestWrapper.isErrorController()) {
            publisher.publishRequestEvent(requestWrapper, responseWrapper);
        }

        return true;
    }
}