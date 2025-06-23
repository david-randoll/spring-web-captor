package com.davidrandoll.spring_web_captor.publisher.request;

import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.davidrandoll.spring_web_captor.utils.ExceptionUtils.safe;
import static java.util.Objects.nonNull;

public class CachedBodyHttpServletRequest extends ContentCachingRequestWrapper {
    private byte[] cachedBody;
    @Getter
    @Setter
    private boolean endpointExists;

    private boolean isPublished = false;
    private HttpRequestEvent httpRequestEvent;
    private final ObjectMapper objectMapper;

    public CachedBodyHttpServletRequest(HttpServletRequest request, ObjectMapper objectMapper) {
        super(request);
        this.objectMapper = objectMapper;
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
    public JsonNode getBody() {
        if (this.cachedBody == null) {
            getInputStream(); // Ensure the body is cached
        }
        return HttpServletUtils.parseByteArrayToJsonNode(this, this.cachedBody, objectMapper);
    }

    public MultiValueMap<String, String> getRequestParams() {
        return this.getParameterMap()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? Arrays.asList(entry.getValue()) : Collections.emptyList(),
                        (a, b) -> b,
                        LinkedMultiValueMap::new
                ));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPathVariables() {
        var pathVariables = this.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables instanceof Map<?, ?>) {
            return (Map<String, String>) pathVariables;
        }
        return Collections.emptyMap();
    }

    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(this.getHeaderNames()).forEach(
                name -> headers.put(name, Collections.list(this.getHeaders(name)))
        );
        return headers;
    }


    public String getPath() {
        return this.getRequestURI();
    }

    public String getFullUrl() {
        return this.getRequestURL().toString();
    }

    public HttpRequestEvent toHttpRequestEvent() {
        if (nonNull(this.httpRequestEvent)) return this.httpRequestEvent;

        this.httpRequestEvent = HttpRequestEvent.builder()
                .endpointExists(this.endpointExists)
                .fullUrl(this.getFullUrl())
                .path(this.getPath())
                .method(safe(() -> HttpMethodEnum.fromValue(this.getMethod()), HttpMethodEnum.UNKNOWN))
                .headers(safe(this::getHttpHeaders, new HttpHeaders()))
                .queryParams(safe(this::getRequestParams, new LinkedMultiValueMap<>()))
                .pathParams(safe(this::getPathVariables, Collections.emptyMap()))
                .requestBody(safe(this::getBody, null))
                .build();

        return this.httpRequestEvent;
    }

    public void publishEvent(List<IHttpEventExtension> httpEventExtensions, IWebCaptorEventPublisher publisher) {
        if (this.isPublished) return;
        var requestEvent = this.toHttpRequestEvent();
        for (IHttpEventExtension extension : httpEventExtensions) {
            var additionalData = extension.extendRequestEvent(requestEvent);
            requestEvent.addAdditionalData(additionalData);
        }
        publisher.publishEvent(requestEvent);
        this.isPublished = true;
    }
}