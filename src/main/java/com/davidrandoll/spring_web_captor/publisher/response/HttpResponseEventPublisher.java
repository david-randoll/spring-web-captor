package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
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
    private final IBodyParserRegistry bodyParserRegistry;

    /**
     * NOTE: Cannot publish the request event here because the path params are not available here yet.
     * After the filter chain is executed, the path params are available in the requestWrapper object.
     * This is why in the {@link  HttpRequestEventPublisher#preHandle}, the event is published in the preHandle method.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException, ServletException {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(
                response, requestWrapper
        );

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            publishRequestEventIfNotPublishedAlready(requestWrapper, responseWrapper);
            responseWrapper.getResponseBody()
                    .thenRun(() -> publisher.publishResponseEvent(requestWrapper, responseWrapper));
        } catch (Exception ex) {
            responseWrapper.getResponseBody()
                    .completeExceptionally(ex);
            throw ex;
        } finally {
            responseWrapper.copyBodyToResponse(); // IMPORTANT: copy response back into original response
        }
    }

    /**
     * There are some times when the {@link HttpRequestEventPublisher#preHandle} method is not called,
     * for example, when the endpoint does not exist or when the request return a 4xx error before the filter chain is executed.
     * In this case, we need to publish the request event here.
     *
     * @param request  the request wrapper that contains the request event
     * @param response the response wrapper that contains the response event
     */
    private void publishRequestEventIfNotPublishedAlready(CachedBodyHttpServletRequest request, CachedBodyHttpServletResponse response) {
        publisher.publishRequestEvent(request, response);
    }
}