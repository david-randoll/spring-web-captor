package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.registry.IFieldCaptorRegistry;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;

@Slf4j
public class CachedBodyHttpServletResponse extends ContentCachingResponseWrapper {
    @Getter
    private final CachedBodyHttpServletRequest request;
    private final IBodyParserRegistry bodyParserRegistry;
    private final IFieldCaptorRegistry fieldCaptorRegistry;

    @Getter
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
                    responseBodyFuture.completeExceptionally(asyncEvent.getThrowable());
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

    public void resolveException(Exception ex, HandlerExceptionResolver resolver) {
        this.resetBuffer();
        this.setContentType("application/json;charset=UTF-8");
        resolver.resolveException(this.request, this, null, ex);
    }

    public void markAsPublished() {
        this.isPublished = true;
    }
}