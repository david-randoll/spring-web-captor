package com.davidrandoll.spring_web_captor.field_captor;

import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import jakarta.servlet.http.HttpServletResponse;

public interface IResponseFieldCaptor {
    void capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder);
}