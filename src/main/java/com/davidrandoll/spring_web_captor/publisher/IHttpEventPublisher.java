package com.davidrandoll.spring_web_captor.publisher;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IHttpEventPublisher {
    void publishRequestEvent(HttpRequestEvent requestEvent,
                             HttpServletRequest request, HttpServletResponse response);

    void publishResponseEvent(HttpRequestEvent requestEvent, HttpResponseEvent responseEvent,
                              HttpServletRequest request, HttpServletResponse response);
}
