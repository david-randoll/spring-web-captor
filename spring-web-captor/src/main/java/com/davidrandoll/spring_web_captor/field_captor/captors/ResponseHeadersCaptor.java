package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.util.Optional;

public class ResponseHeadersCaptor implements IResponseFieldCaptor {
    @Override
    public void capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder) {
        HttpHeaders headers = this.getHttpHeaders(response);
        builder.responseHeaders(headers);
    }

    public HttpHeaders getHttpHeaders(HttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        Optional.of(response.getHeaderNames())
                .ifPresent(headerNames -> headerNames.forEach(name -> headers.put(name, response.getHeaders(name).stream().toList())));
        return headers;
    }
}