package com.davidrandoll.spring_web_captor.extensions;


import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface IHttpEventExtension {
    /**
     * This method is called before the {@link HttpRequestEvent} is published.
     * It can be used to add additional data ({@link HttpRequestEvent#setAdditionalData}) to the request event.
     * <p>
     * For example, anything from spring security (username, roles, etc). Or tenantId if in a multi-tenant environment.
     * </p>
     */
    default Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        return Map.of();
    }

    /**
     * This method is called before the {@link HttpResponseEvent} is published.
     * It can be used to add additional data ({@link HttpResponseEvent#setAdditionalData}) to the response event.
     * <p>
     * For example, anything from spring security (username, roles, etc). Or tenantId if in a multi-tenant environment.
     * </p>
     */
    default Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        return Map.of();
    }
}