package com.davidrandoll.spring_web_captor.publisher.request;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.registry.IFieldCaptorRegistry;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

import static java.util.Objects.nonNull;

@Slf4j
public class CachedBodyHttpServletRequest extends ContentCachingRequestWrapper {
    private byte[] cachedBody;
    @Getter
    @Setter
    private boolean endpointCalled;

    @Getter
    private boolean isPublished = false;
    private HttpRequestEvent httpRequestEvent;

    public CachedBodyHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public boolean isErrorController(){
        return this.getRequestURI().equalsIgnoreCase("/error");
    }

    @Override
    @NonNull
    public ServletInputStream getInputStream() throws IOException {
        if (this.cachedBody != null) {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

        var cached = super.getContentAsByteArray();
        if (cached.length > 0) {
            this.cachedBody = cached;
        } else {
            this.cachedBody = readUncached();
        }
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    /**
     * The body nobody read during the chain. When the container has already closed the stream - a body-less
     * request, or an empty body that a reader consumed and auto-closed, so nothing was cached - there is no body
     * left to capture, and that is an empty body rather than an error.
     */
    private byte[] readUncached() throws IOException {
        ServletInputStream inputStream = super.getInputStream();
        try {
            return StreamUtils.copyToByteArray(inputStream);
        } catch (IOException closed) {
            log.debug("Request body not readable after the chain ({}); captured as empty", closed.getMessage());
            return new byte[0];
        }
    }

    @SneakyThrows
    public byte[] getCachedBody() {
        if (this.cachedBody == null) {
            getInputStream();
        }

        return this.cachedBody;
    }

    public HttpRequestEvent toHttpRequestEvent(IFieldCaptorRegistry registry) {
        if (nonNull(this.httpRequestEvent)) return this.httpRequestEvent;

        this.httpRequestEvent = registry
                .capture(this, HttpRequestEvent.builder())
                .build();

        return this.httpRequestEvent;
    }

    public void markAsPublished() {
        this.isPublished = true;
    }
}