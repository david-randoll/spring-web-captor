package com.davidrandoll.spring_web_captor.field_captor;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import jakarta.servlet.http.HttpServletRequest;

public interface IRequestFieldCaptor {
    void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder);
}