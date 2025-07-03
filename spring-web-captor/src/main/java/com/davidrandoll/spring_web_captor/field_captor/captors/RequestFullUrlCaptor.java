package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;

public class RequestFullUrlCaptor implements IRequestFieldCaptor {
    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        var url = request.getRequestURL().toString();
        builder.fullUrl(url);
    }
}