package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Collections;

public class HeadersRequestCaptor implements IRequestFieldCaptor {
    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        HttpHeaders headers = this.getHttpHeaders(request);
        builder.headers(headers);
    }

    public HttpHeaders getHttpHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(
                name -> headers.put(name, Collections.list(request.getHeaders(name)))
        );
        return headers;
    }
}