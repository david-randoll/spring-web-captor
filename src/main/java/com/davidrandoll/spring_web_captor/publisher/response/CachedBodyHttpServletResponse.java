package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.registry.IFieldCaptorRegistry;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
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

    @Getter
    private boolean isPublished = false;
    private HttpResponseEvent httpResponseEvent;
    private CompletableFuture<byte[]> responseBodyFuture;

    public CachedBodyHttpServletResponse(HttpServletResponse response, CachedBodyHttpServletRequest request) {
        super(response);
        this.request = request;
    }

    public CompletableFuture<byte[]> getResponseBody() throws IOException {
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

    private void getBody(CompletableFuture<byte[]> future) throws IOException {
        future.complete(this.getContentAsByteArray());
        this.copyBodyToResponse(); // IMPORTANT: copy response back into original response
    }

    public HttpStatus getResponseStatus() {
        return HttpStatus.valueOf(this.getStatus());
    }

    public HttpResponseEvent toHttpResponseEvent(IFieldCaptorRegistry fieldCaptorRegistry) {
        if (nonNull(this.httpResponseEvent)) return this.httpResponseEvent;

        HttpRequestEvent requestEvent = this.request.toHttpRequestEvent(fieldCaptorRegistry);
        this.httpResponseEvent = fieldCaptorRegistry
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