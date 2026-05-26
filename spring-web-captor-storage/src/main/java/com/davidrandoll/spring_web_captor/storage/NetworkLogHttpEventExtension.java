package com.davidrandoll.spring_web_captor.storage;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Injects the {@code requestId} into the captor event's {@code additionalData}
 * so the listener can correlate the request and response phases of the same
 * HTTP exchange.
 */
@RequiredArgsConstructor
public class NetworkLogHttpEventExtension implements IHttpEventExtension {

    public static final String REQUEST_ID_KEY = "networkLogRequestId";

    private final RequestIdProvider requestIdProvider;

    @Override
    public Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        // Reuse an existing requestId if the captor already saw this request on an earlier
        // dispatch (e.g. an original dispatch followed by a Tomcat error dispatch via sendError).
        // The same requestId lets the consumer correlate both publications into one row.
        Object existing = req.getAttribute(REQUEST_ID_KEY);
        String requestId = (existing instanceof String s && !s.isEmpty()) ? s : requestIdProvider.nextRequestId(req);
        req.setAttribute(REQUEST_ID_KEY, requestId);

        Map<String, Object> data = new HashMap<>();
        data.put(REQUEST_ID_KEY, requestId);
        return data;
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        Map<String, Object> data = new HashMap<>();
        Object requestId = req.getAttribute(REQUEST_ID_KEY);
        if (requestId != null) {
            data.put(REQUEST_ID_KEY, requestId.toString());
        }
        return data;
    }
}
