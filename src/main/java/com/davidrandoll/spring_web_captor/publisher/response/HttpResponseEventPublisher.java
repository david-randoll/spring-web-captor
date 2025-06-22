package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    private final ObjectMapper objectMapper;

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
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request, objectMapper);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper, objectMapper);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            buildAndPublishResponseEvent(requestWrapper, responseWrapper);
        } catch (Exception ex) {
            responseWrapper.resolveException(ex, handlerExceptionResolver);
        } finally {
            responseWrapper.copyBodyToResponse(); // IMPORTANT: copy response back into original response
        }
    }

    private void buildAndPublishResponseEvent(CachedBodyHttpServletRequest requestWrapper, CachedBodyHttpServletResponse responseWrapper) throws IOException {
        final HttpStatus responseStatus = responseWrapper.getResponseStatus();
        final HttpResponseEvent responseEvent = responseWrapper.toHttpResponseEvent();

        if (responseStatus.is2xxSuccessful()) {
            CompletionStage<JsonNode> responseBody = responseWrapper.getResponseBody();

            responseBody.whenComplete((body, throwable) -> {
                if (throwable != null) {
                    var ex = new RuntimeException(throwable);
                    responseWrapper.resolveException(ex, handlerExceptionResolver);
                    publishErrorResponseEvent(requestWrapper, responseEvent, responseWrapper);
                } else {
                    responseEvent.setResponseBody(body);
                    responseWrapper.publishEvent(httpEventExtensions, publisher);
                }
            });
        } else {
            publishErrorResponseEvent(requestWrapper, responseEvent, responseWrapper);
        }
    }

    private void publishErrorResponseEvent(CachedBodyHttpServletRequest requestWrapper, HttpResponseEvent responseEvent, CachedBodyHttpServletResponse responseWrapper) {
        WebRequest webRequest = new ServletWebRequest(requestWrapper);
        ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.values());
        Map<String, Object> errorAttributes = defaultErrorAttributes.getErrorAttributes(webRequest, options);
        responseEvent.addErrorDetail(errorAttributes);
        responseWrapper.publishEvent(httpEventExtensions, publisher);
    }
}