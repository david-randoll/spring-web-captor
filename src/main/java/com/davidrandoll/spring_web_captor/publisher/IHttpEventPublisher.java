package com.davidrandoll.spring_web_captor.publisher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IHttpEventPublisher {

    void publishRequestEvent(HttpServletRequest request, HttpServletResponse response);

    void publishResponseEvent(HttpServletRequest request, HttpServletResponse response);
}
