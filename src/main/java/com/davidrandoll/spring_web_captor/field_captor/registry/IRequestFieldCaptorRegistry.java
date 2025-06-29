package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;

public interface IRequestFieldCaptorRegistry {
    void register(IRequestFieldCaptor captor);

    HttpRequestEvent.HttpRequestEventBuilder<?, ?> capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder);
}