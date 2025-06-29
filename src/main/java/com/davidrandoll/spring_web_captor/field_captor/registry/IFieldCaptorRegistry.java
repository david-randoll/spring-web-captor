package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IFieldCaptorRegistry {
    void register(IRequestFieldCaptor captor);

    void register(IResponseFieldCaptor captor);

    HttpRequestEvent.HttpRequestEventBuilder<?, ?> capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder);

    HttpResponseEvent.HttpResponseEventBuilder<?, ?> capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder);
}