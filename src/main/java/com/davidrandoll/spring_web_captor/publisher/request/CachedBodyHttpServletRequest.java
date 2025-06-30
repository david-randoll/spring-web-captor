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
    private final IFieldCaptorRegistry registry;

    private byte[] cachedBody;
    @Getter
    @Setter
    private boolean endpointExists;

    @Getter
    private boolean isPublished = false;
    private HttpRequestEvent httpRequestEvent;

    public CachedBodyHttpServletRequest(HttpServletRequest request, IFieldCaptorRegistry registry) {
        super(request);
        this.registry = registry;
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
            var inputStream = super.getInputStream();
            this.cachedBody = StreamUtils.copyToByteArray(inputStream);
        }
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @SneakyThrows
    public byte[] getCachedBody() {
        if (this.cachedBody == null) {
            getInputStream();
        }

        return this.cachedBody;
    }

    public HttpRequestEvent toHttpRequestEvent() {
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