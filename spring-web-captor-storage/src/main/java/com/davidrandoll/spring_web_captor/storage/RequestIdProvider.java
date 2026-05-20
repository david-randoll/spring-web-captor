package com.davidrandoll.spring_web_captor.storage;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Strategy for generating the {@code requestId} that ties the request-phase
 * row to its response-phase update. Default returns a random UUID; override
 * to read an inbound tracing header instead.
 */
@FunctionalInterface
public interface RequestIdProvider {

    String nextRequestId(HttpServletRequest request);

    static RequestIdProvider uuid() {
        return req -> UUID.randomUUID().toString();
    }
}
