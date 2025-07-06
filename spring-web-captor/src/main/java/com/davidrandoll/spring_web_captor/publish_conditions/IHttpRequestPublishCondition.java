package com.davidrandoll.spring_web_captor.publish_conditions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IHttpRequestPublishCondition {
    /**
     * Determines whether the request event should be published.
     */
    boolean shouldPublishRequest(HttpServletRequest request, HttpServletResponse response);
}