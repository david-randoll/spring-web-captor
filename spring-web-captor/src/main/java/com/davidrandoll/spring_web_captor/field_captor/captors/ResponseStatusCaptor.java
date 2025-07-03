package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

public class ResponseStatusCaptor implements IResponseFieldCaptor {
    @Override
    public void capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder) {
        HttpStatus responseStatus = HttpStatus.valueOf(response.getStatus());
        builder.responseStatus(responseStatus);
    }
}