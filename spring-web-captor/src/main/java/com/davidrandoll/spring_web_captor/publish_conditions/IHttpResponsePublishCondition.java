package com.davidrandoll.spring_web_captor.publish_conditions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IHttpResponsePublishCondition {
    /**
     * Determines whether the response event should be published.
     */
    boolean shouldPublishResponse(HttpServletRequest request, HttpServletResponse response);
}