package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.field_captor.registry.IFieldCaptorRegistry;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;

@Slf4j
public class CachedBodyHttpServletResponse extends ContentCachingResponseWrapper {
    private final CachedBodyHttpServletRequest request;
    private final IBodyParserRegistry bodyParserRegistry;
    private final IFieldCaptorRegistry fieldCaptorRegistry;

    private boolean isPublished = false;
    private HttpResponseEvent httpResponseEvent;
    private CompletableFuture<JsonNode> responseBodyFuture;

    public CachedBodyHttpServletResponse(HttpServletResponse response, CachedBodyHttpServletRequest request, IBodyParserRegistry bodyParserRegistry, IFieldCaptorRegistry fieldCaptorRegistry) {
        super(response);
        this.request = request;
        this.bodyParserRegistry = bodyParserRegistry;
        this.fieldCaptorRegistry = fieldCaptorRegistry;
    }

    public CompletableFuture<JsonNode> getResponseBody() throws IOException {
        if (this.responseBodyFuture != null) return this.responseBodyFuture;

        this.responseBodyFuture = new CompletableFuture<>();

        if (request.isAsyncStarted()) {
            request.getAsyncContext().addListener(new AsyncListener() {
                public void onComplete(AsyncEvent asyncEvent) throws IOException {
                    getBody(responseBodyFuture);
                }

                public void onTimeout(AsyncEvent asyncEvent) {
                    //ignore
                }

                public void onError(AsyncEvent asyncEvent) {
                    //ignore
                    log.error("Error occurred while processing async request", asyncEvent.getThrowable());
                }

                public void onStartAsync(AsyncEvent asyncEvent) {
                    //ignore
                }
            });
        } else {
            getBody(responseBodyFuture);
        }
        return responseBodyFuture;
    }

    private void getBody(CompletableFuture<JsonNode> future) throws IOException {
        BodyPayload payload = this.bodyParserRegistry.parse(
                this.request.getRequest(),
                this.getContentAsByteArray()
        );
        future.complete(payload.getBody());
        this.copyBodyToResponse(); // IMPORTANT: copy response back into original response
    }

    public HttpStatus getResponseStatus() {
        return HttpStatus.valueOf(this.getStatus());
    }

    public HttpResponseEvent toHttpResponseEvent() {
        if (nonNull(this.httpResponseEvent)) return this.httpResponseEvent;

        HttpRequestEvent requestEvent = this.request.toHttpRequestEvent();
        this.httpResponseEvent = this.fieldCaptorRegistry
                .capture(this, new HttpResponseEvent(requestEvent).toBuilder())
                .build();

        return this.httpResponseEvent;
    }

    public void publishEvent(List<IHttpEventExtension> httpEventExtensions, IWebCaptorEventPublisher publisher) {
        if (this.isPublished) return;
        HttpRequestEvent requestEvent = this.request.toHttpRequestEvent();
        HttpResponseEvent responseEvent = this.toHttpResponseEvent();
        for (IHttpEventExtension extension : httpEventExtensions) {
            try {
                var additionalData = extension.enrichResponseEvent(this.request, this, requestEvent, responseEvent);
                responseEvent.addAdditionalData(additionalData);
            } catch (Exception e) {
                log.error("Error enriching response event with extension: {}", extension.getClass().getName(), e);
            }
        }
        publisher.publishEvent(responseEvent);
        this.isPublished = true;
    }

    public void resolveException(Exception ex, HandlerExceptionResolver handlerExceptionResolver) {
        this.resetBuffer();
        this.setContentType("application/json;charset=UTF-8");
        handlerExceptionResolver.resolveException(this.request, this, null, ex);
    }

    public void publishErrorEvent(List<IHttpEventExtension> httpEventExtensions, IWebCaptorEventPublisher publisher, DefaultErrorAttributes defaultErrorAttributes) {
        HttpResponseEvent responseEvent = this.toHttpResponseEvent();
        WebRequest webRequest = new ServletWebRequest(this.request);
        ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.values());
        Map<String, Object> errorAttributes = defaultErrorAttributes.getErrorAttributes(webRequest, options);
        responseEvent.addErrorDetail(errorAttributes);
        this.publishEvent(httpEventExtensions, publisher);
    }
}