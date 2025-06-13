package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
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
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, objectMapper);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            buildAndPublishResponseEvent(requestWrapper, responseWrapper);
        } catch (Exception ex) {
            resolveException(ex, responseWrapper, requestWrapper);
        } finally {
            responseWrapper.copyBodyToResponse(); // IMPORTANT: copy response back into original response
        }
    }

    private void buildAndPublishResponseEvent(CachedBodyHttpServletRequest requestWrapper, CachedBodyHttpServletResponse responseWrapper) {
        final HttpStatus responseStatus = HttpStatus.valueOf(responseWrapper.getStatus());
        final HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent();
        final HttpResponseEvent responseEvent = createHttpResponseEvent(requestEvent, responseStatus, responseWrapper);

        if (responseStatus.is2xxSuccessful()) {
            CompletionStage<JsonNode> responseBody = responseWrapper.getResponseBody(requestWrapper);

            responseBody.whenComplete((body, throwable) -> {
                if (throwable != null) {
                    var ex = new RuntimeException(throwable);
                    resolveException(ex, responseWrapper, requestWrapper);
                } else {
                    responseEvent.setResponseBody(body);
                }
                publishResponseEvent(requestEvent, responseEvent);
            });
        } else {
            var errorAttributes = getErrorAttributes(requestWrapper);
            responseEvent.addErrorDetail(errorAttributes);
            publishResponseEvent(requestEvent, responseEvent);
        }
    }

    private void publishResponseEvent(HttpRequestEvent requestEvent, HttpResponseEvent responseEvent) {
        for (IHttpEventExtension extension : httpEventExtensions) {
            var additionalData = extension.extendResponseEvent(requestEvent, responseEvent);
            responseEvent.addAdditionalData(additionalData);
        }
        publisher.publishEvent(responseEvent);
    }

    private static HttpResponseEvent createHttpResponseEvent(HttpRequestEvent requestEvent, HttpStatus responseStatus, CachedBodyHttpServletResponse responseWrapper) {
        var responseHeaders = responseWrapper.getHttpHeaders();

        return new HttpResponseEvent(requestEvent).toBuilder()
                .responseStatus(responseStatus)
                .responseHeaders(responseHeaders)
                .build();
    }

    private void resolveException(Exception ex, CachedBodyHttpServletResponse responseWrapper, CachedBodyHttpServletRequest requestWrapper) {
        responseWrapper.resetBuffer();
        responseWrapper.setContentType("application/json;charset=UTF-8");
        handlerExceptionResolver.resolveException(requestWrapper, responseWrapper, null, ex);
    }

    private Map<String, Object> getErrorAttributes(CachedBodyHttpServletRequest requestWrapper) {
        WebRequest webRequest = new ServletWebRequest(requestWrapper);
        ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.values());
        return defaultErrorAttributes.getErrorAttributes(webRequest, options);
    }
}