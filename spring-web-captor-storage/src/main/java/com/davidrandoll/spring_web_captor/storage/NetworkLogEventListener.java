package com.davidrandoll.spring_web_captor.storage;

import com.davidrandoll.spring_web_captor.event.BaseHttpEvent;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Funnels captor request/response events into the consumer-supplied
 * {@link INetworkLogStore}. Pluggable behaviour:
 * <ul>
 *     <li>{@link NetworkLogProperties} controls capture / redaction / exclusion;</li>
 *     <li>{@link NetworkLogEnricher} beans add context-specific fields;</li>
 *     <li>{@link RequestIdProvider} (used by {@link NetworkLogHttpEventExtension})
 *         controls the correlation id.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class NetworkLogEventListener {

    private final INetworkLogStore store;
    private final NetworkLogProperties properties;
    private final ObjectMapper objectMapper;
    private final List<NetworkLogEnricher> enrichers;

    @Async
    @EventListener
    public void onHttpRequest(HttpRequestEvent event) {
        if (!properties.isEnabled() || isExcludedPath(event.getPath())) return;
        try {
            INetworkLog networkLog = store.newInstance();
            networkLog.setRequestId(extractRequestId(event.getAdditionalData()))
                    .setRequestTimestamp(Instant.now())
                    .setMethod(event.getMethod() != null ? event.getMethod().name() : null)
                    .setFullUrl(event.getFullUrl())
                    .setPath(event.getPath())
                    .setRequestHeaders(redactHeaders(toMap(event.getHeaders())))
                    .setQueryParams(multiValueMapToMap(event.getQueryParams()))
                    .setPathParams(event.getPathParams() != null ? new HashMap<>(event.getPathParams()) : null)
                    .setRequestBody(redactFields(event.getRequestBody()))
                    .setAdditionalData(redactMapFields(event.getAdditionalData()))
                    .setEndpointCalled(event.isEndpointCalled());

            applyEnrichers(networkLog, event);
            store.save(networkLog);
        } catch (Exception e) {
            log.error("Failed to persist network request log for {} {}: {}",
                    event.getMethod(), event.getPath(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onHttpResponse(HttpResponseEvent event) {
        if (!properties.isEnabled() || isExcludedPath(event.getPath())) return;
        try {
            Integer status = event.getResponseStatus() != null ? event.getResponseStatus().value() : null;
            String requestId = extractRequestId(event.getAdditionalData());

            var decision = CaptureRuleResolver.resolve(status, properties);
            if (!decision.capture()) {
                if (requestId != null) {
                    store.deleteByRequestId(requestId);
                }
                return;
            }

            INetworkLog networkLog = requestId != null
                    ? store.findByRequestId(requestId).orElse(null)
                    : null;

            if (networkLog != null) {
                networkLog
                        .setResponseStatus(status)
                        .setResponseHeaders(redactHeaders(toMap(event.getResponseHeaders())))
                        .setResponseBody(redactFields(event.getResponseBody()))
                        .setErrorDetail(event.getErrorDetail())
                        .setAdditionalData(redactMapFields(mergeAdditionalData(networkLog.getAdditionalData(), event.getAdditionalData())));
            } else {
                networkLog = store.newInstance();
                networkLog.setRequestId(requestId)
                        .setRequestTimestamp(Instant.now())
                        .setMethod(event.getMethod() != null ? event.getMethod().name() : null)
                        .setFullUrl(event.getFullUrl())
                        .setPath(event.getPath())
                        .setRequestHeaders(redactHeaders(toMap(event.getHeaders())))
                        .setQueryParams(multiValueMapToMap(event.getQueryParams()))
                        .setPathParams(event.getPathParams() != null ? new HashMap<>(event.getPathParams()) : null)
                        .setRequestBody(redactFields(event.getRequestBody()))
                        .setAdditionalData(redactMapFields(event.getAdditionalData()))
                        .setEndpointCalled(event.isEndpointCalled())
                        .setResponseStatus(status)
                        .setResponseHeaders(redactHeaders(toMap(event.getResponseHeaders())))
                        .setResponseBody(redactFields(event.getResponseBody()))
                        .setErrorDetail(event.getErrorDetail());
            }

            applyEnrichers(networkLog, event);
            NetworkLogFieldMask.apply(networkLog, decision.fieldWhitelist());
            store.save(networkLog);
        } catch (Exception e) {
            log.error("Failed to persist network response log for {} {}: {}",
                    event.getMethod(), event.getPath(), e.getMessage(), e);
        }
    }

    private void applyEnrichers(INetworkLog log, BaseHttpEvent event) {
        if (enrichers == null) return;
        for (NetworkLogEnricher enricher : enrichers) {
            try {
                enricher.enrich(log, event);
            } catch (Exception e) {
                NetworkLogEventListener.log.warn("NetworkLogEnricher {} failed: {}",
                        enricher.getClass().getName(), e.getMessage());
            }
        }
    }

    private boolean isExcludedPath(String path) {
        if (path == null || properties.getExcludePaths() == null || properties.getExcludePaths().isEmpty()) {
            return false;
        }
        return properties.getExcludePaths().stream().anyMatch(path::startsWith);
    }

    private String extractRequestId(Map<String, Object> additionalData) {
        if (additionalData == null) return null;
        Object requestId = additionalData.get(NetworkLogHttpEventExtension.REQUEST_ID_KEY);
        return requestId != null ? requestId.toString() : null;
    }

    private Map<String, Object> toMap(HttpHeaders headers) {
        if (headers == null) return null;
        return new HashMap<>(headers.toSingleValueMap());
    }

    private Map<String, Object> multiValueMapToMap(MultiValueMap<String, String> multiValueMap) {
        if (multiValueMap == null) return null;
        return new HashMap<>(multiValueMap.toSingleValueMap());
    }

    private Map<String, Object> mergeAdditionalData(Map<String, Object> existing, Map<String, Object> incoming) {
        if (existing == null) return incoming;
        if (incoming == null) return existing;
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(incoming);
        return merged;
    }

    private Map<String, Object> redactHeaders(Map<String, Object> headers) {
        if (headers == null || properties.getRedactHeaders() == null || properties.getRedactHeaders().isEmpty()) {
            return headers;
        }
        Set<String> redactSet = properties.getRedactHeaders().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        headers.replaceAll((key, value) -> redactSet.contains(key.toLowerCase()) ? "[REDACTED]" : value);
        return headers;
    }

    private JsonNode redactFields(JsonNode node) {
        if (node == null || properties.getRedactFields() == null || properties.getRedactFields().isEmpty()) {
            return node;
        }
        Set<String> fieldNames = Set.copyOf(properties.getRedactFields());
        redactFieldsRecursive(node, fieldNames);
        return node;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> redactMapFields(Map<String, Object> map) {
        if (map == null || properties.getRedactFields() == null || properties.getRedactFields().isEmpty()) {
            return map;
        }
        JsonNode node = objectMapper.valueToTree(map);
        redactFieldsRecursive(node, Set.copyOf(properties.getRedactFields()));
        return objectMapper.convertValue(node, Map.class);
    }

    private void redactFieldsRecursive(JsonNode node, Set<String> fieldNames) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(name -> {
                if (fieldNames.contains(name)) {
                    obj.put(name, "[REDACTED]");
                } else {
                    redactFieldsRecursive(obj.get(name), fieldNames);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                redactFieldsRecursive(element, fieldNames);
            }
        }
    }
}
