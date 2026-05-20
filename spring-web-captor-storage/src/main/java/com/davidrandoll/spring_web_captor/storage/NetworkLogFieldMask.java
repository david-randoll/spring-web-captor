package com.davidrandoll.spring_web_captor.storage;

import java.util.Set;

final class NetworkLogFieldMask {

    private static final Set<String> ALWAYS_KEEP = Set.of(
            "requestId", "requestTimestamp", "responseStatus");

    private NetworkLogFieldMask() {
    }

    static void apply(INetworkLog log, Set<String> whitelist) {
        if (log == null || whitelist == null || whitelist.isEmpty()) return;
        if (!keep("method", whitelist))          log.setMethod(null);
        if (!keep("fullUrl", whitelist))         log.setFullUrl(null);
        if (!keep("path", whitelist))            log.setPath(null);
        if (!keep("endpointCalled", whitelist))  log.setEndpointCalled(null);
        if (!keep("requestHeaders", whitelist))  log.setRequestHeaders(null);
        if (!keep("queryParams", whitelist))     log.setQueryParams(null);
        if (!keep("pathParams", whitelist))      log.setPathParams(null);
        if (!keep("requestBody", whitelist))     log.setRequestBody(null);
        if (!keep("additionalData", whitelist))  log.setAdditionalData(null);
        if (!keep("responseHeaders", whitelist)) log.setResponseHeaders(null);
        if (!keep("responseBody", whitelist))    log.setResponseBody(null);
        if (!keep("errorDetail", whitelist))     log.setErrorDetail(null);
    }

    private static boolean keep(String fieldName, Set<String> whitelist) {
        return ALWAYS_KEEP.contains(fieldName) || whitelist.contains(fieldName);
    }
}
