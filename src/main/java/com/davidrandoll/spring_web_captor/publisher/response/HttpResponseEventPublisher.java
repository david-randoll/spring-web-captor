package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;


@Slf4j
@Component("httpResponseEventPublisher")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnMissingBean(name = "httpResponseEventPublisher", ignored = HttpResponseEventPublisher.class)
public class HttpResponseEventPublisher extends OncePerRequestFilter {
    private final IWebCaptorEventPublisher publisher;
    private final DefaultErrorAttributes defaultErrorAttributes;
    private final List<IHttpEventExtension> httpEventExtensions;
    private final IBodyParserRegistry bodyParserRegistry;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver handlerExceptionResolver;

    /**
     * NOTE: Cannot publish the request event here because the path params are not available here yet.
     * After the filter chain is executed, the path params are available in the requestWrapper object.
     * This is why in the {@link  HttpRequestEventPublisher#preHandle}, the event is published in the preHandle method.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request, bodyParserRegistry);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper, bodyParserRegistry);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            publishRequestEventIfNotPublishedAlready(requestWrapper, responseWrapper);
            buildAndPublishResponseEvent(responseWrapper);
        } catch (Exception ex) {
            responseWrapper.resolveException(ex, handlerExceptionResolver);
            responseWrapper.publishErrorEvent(httpEventExtensions, publisher, defaultErrorAttributes);
        } finally {
            responseWrapper.copyBodyToResponse(); // IMPORTANT: copy response back into original response
        }
    }

    /**
     * There are some times when the {@link HttpRequestEventPublisher#preHandle} method is not called,
     * for example, when the endpoint does not exist or when the request return a 4xx error before the filter chain is executed.
     * In this case, we need to publish the request event here.
     *
     * @param requestWrapper  the request wrapper that contains the request event
     * @param responseWrapper the response wrapper that contains the response event
     */
    private void publishRequestEventIfNotPublishedAlready(CachedBodyHttpServletRequest requestWrapper, CachedBodyHttpServletResponse responseWrapper) {
        requestWrapper.publishEvent(httpEventExtensions, publisher, responseWrapper);
    }

    private void buildAndPublishResponseEvent(CachedBodyHttpServletResponse responseWrapper) throws IOException {
        final HttpStatus responseStatus = responseWrapper.getResponseStatus();
        if (responseStatus.is2xxSuccessful()) {
            CompletionStage<JsonNode> responseBody = responseWrapper.getResponseBody();

            responseBody.whenComplete((body, throwable) -> {
                if (throwable != null) {
                    var ex = new RuntimeException(throwable);
                    responseWrapper.resolveException(ex, handlerExceptionResolver);
                    responseWrapper.publishErrorEvent(httpEventExtensions, publisher, defaultErrorAttributes);
                } else {
                    responseWrapper.publishEvent(body, httpEventExtensions, publisher);
                }
            });
        } else {
            responseWrapper.publishErrorEvent(httpEventExtensions, publisher, defaultErrorAttributes);
        }
    }
}