package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.publisher.IHttpEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Slf4j
@RequiredArgsConstructor
public class HttpResponseEventPublisher extends OncePerRequestFilter {
    private final IHttpEventPublisher publisher;

    /**
     * NOTE: Cannot publish the request event here because the path params are not available here yet.
     * After the filter chain is executed, the path params are available in the requestWrapper object.
     * This is why in the {@link  HttpRequestEventPublisher#preHandle}, the event is published in the preHandle method.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException, ServletException {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper);

        boolean shouldPublishRequest = publisher.shouldPublishRequestEvent(requestWrapper, responseWrapper);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            if (!requestWrapper.isPublished() && shouldPublishRequest) {
                // If the request event is not published, we need to publish it here.
                // This can happen if the request does not reach the controller or if an error occurs before the filter chain is executed.
                publisher.publishRequestEvent(request, response);
            }
            boolean shouldPublishResponse = publisher.shouldPublishResponseEvent(requestWrapper, responseWrapper);
            if (shouldPublishResponse) {
                responseWrapper.getResponseBody()
                        .thenRun(() -> publisher.publishResponseEvent(requestWrapper, responseWrapper));
            }
        } catch (Exception ex) {
            responseWrapper.getResponseBody()
                    .completeExceptionally(ex);
            throw ex;
        } finally {
            responseWrapper.copyBodyToResponse(); // IMPORTANT: copy response back into original response
        }
    }
}