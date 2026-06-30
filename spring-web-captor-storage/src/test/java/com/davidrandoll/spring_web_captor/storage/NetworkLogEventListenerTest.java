package com.davidrandoll.spring_web_captor.storage;

import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.storage.support.InMemoryNetworkLogStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.davidrandoll.spring_web_captor.storage.NetworkLogHttpEventExtension.REQUEST_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class NetworkLogEventListenerTest {

    private static final String RID = "req-123";

    private InMemoryNetworkLogStore store;
    private NetworkLogProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<NetworkLogEnricher> enrichers;
    private NetworkLogEventListener listener;

    @BeforeEach
    void setUp() {
        store = new InMemoryNetworkLogStore();
        properties = new NetworkLogProperties();
        enrichers = new ArrayList<>();
        listener = new NetworkLogEventListener(store, properties, objectMapper, enrichers);
    }

    private HttpRequestEvent.HttpRequestEventBuilder<?, ?> requestBuilder() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer secret");
        headers.add("X-Custom", "keep-me");

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("color", "blue");

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "42");

        return HttpRequestEvent.builder()
                .method(HttpMethodEnum.GET)
                .fullUrl("http://localhost/api/things/42?color=blue")
                .path("/api/things/42")
                .headers(headers)
                .queryParams(query)
                .pathParams(pathParams)
                .endpointCalled(true)
                .additionalData(REQUEST_ID_KEY, RID);
    }

    @Test
    void requestPhasePersistsRowWithDefaultAuthorizationRedaction() {
        listener.onHttpRequest(requestBuilder().build());

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getMethod()).isEqualTo("GET");
        assertThat(saved.getPath()).isEqualTo("/api/things/42");
        assertThat(saved.getFullUrl()).contains("color=blue");
        assertThat(saved.getRequestHeaders().get("Authorization")).isEqualTo("[REDACTED]");
        assertThat(saved.getRequestHeaders().get("X-Custom")).isEqualTo("keep-me");
        assertThat(saved.getQueryParams()).containsEntry("color", "blue");
        assertThat(saved.getPathParams()).containsEntry("id", "42");
        assertThat(saved.getEndpointCalled()).isTrue();
        assertThat(saved.getRequestTimestamp()).isNotNull();
    }

    @Test
    void requestPhaseSkippedWhenDisabled() {
        properties.setEnabled(false);
        listener.onHttpRequest(requestBuilder().build());
        assertThat(store.size()).isZero();
    }

    @Test
    void requestPhaseSkippedForExcludedPath() {
        properties.setExcludePaths(List.of("/api/things"));
        listener.onHttpRequest(requestBuilder().build());
        assertThat(store.size()).isZero();
    }

    @Test
    void requestPhaseToleratesStoreFailure() {
        NetworkLogEventListener boom = new NetworkLogEventListener(new InMemoryNetworkLogStore() {
            @Override
            public void save(INetworkLog log) {
                throw new IllegalStateException("db down");
            }
        }, properties, objectMapper, enrichers);
        // should swallow the exception rather than propagate
        boom.onHttpRequest(requestBuilder().build());
    }

    @Test
    void responsePhaseUpdatesExistingRow() {
        listener.onHttpRequest(requestBuilder().build());

        HttpResponseEvent response = HttpResponseEvent.builder()
                .method(HttpMethodEnum.GET)
                .path("/api/things/42")
                .responseStatus(HttpStatus.OK)
                .responseHeaders(new HttpHeaders())
                .additionalData(REQUEST_ID_KEY, RID)
                .build();
        response.setResponseBody(JsonNodeFactory.instance.textNode("ok"));

        listener.onHttpResponse(response);

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getResponseStatus()).isEqualTo(200);
        assertThat(saved.getResponseBody().asText()).isEqualTo("ok");
    }

    @Test
    void responsePhaseCreatesRowWhenNoRequestRowExists() {
        HttpResponseEvent response = HttpResponseEvent.builder()
                .method(HttpMethodEnum.POST)
                .path("/api/orphan")
                .fullUrl("http://localhost/api/orphan")
                .headers(new HttpHeaders())
                .responseStatus(HttpStatus.CREATED)
                .responseHeaders(new HttpHeaders())
                .endpointCalled(true)
                .additionalData(REQUEST_ID_KEY, RID)
                .build();

        listener.onHttpResponse(response);

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getMethod()).isEqualTo("POST");
        assertThat(saved.getResponseStatus()).isEqualTo(201);
    }

    @Test
    void responsePhaseDeletesRowWhenCaptureRuleRejects() {
        // Only capture >=500; a 200 response should drop the previously stored request row.
        properties.setCaptureStatuses(List.of(">=500"));
        listener.onHttpRequest(requestBuilder().build());
        assertThat(store.size()).isEqualTo(1);

        HttpResponseEvent response = HttpResponseEvent.builder()
                .path("/api/things/42")
                .responseStatus(HttpStatus.OK)
                .additionalData(REQUEST_ID_KEY, RID)
                .build();
        listener.onHttpResponse(response);

        assertThat(store.findByRequestId(RID)).isEmpty();
    }

    @Test
    void responsePhaseAppliesFieldWhitelistMask() {
        NetworkLogProperties.CaptureRule rule = new NetworkLogProperties.CaptureRule();
        rule.setMatch(List.of(">=400"));
        rule.setFields(List.of("path"));
        properties.setRules(List.of(rule));

        listener.onHttpRequest(requestBuilder().build());

        HttpResponseEvent response = HttpResponseEvent.builder()
                .method(HttpMethodEnum.GET)
                .path("/api/things/42")
                .responseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .responseHeaders(new HttpHeaders())
                .additionalData(REQUEST_ID_KEY, RID)
                .build();
        listener.onHttpResponse(response);

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getPath()).isEqualTo("/api/things/42"); // whitelisted
        assertThat(saved.getMethod()).isNull();                  // masked
        assertThat(saved.getResponseStatus()).isEqualTo(500);    // always kept
    }

    @Test
    void responsePhaseSkippedForExcludedPath() {
        properties.setExcludePaths(List.of("/api"));
        HttpResponseEvent response = HttpResponseEvent.builder()
                .path("/api/x")
                .responseStatus(HttpStatus.OK)
                .additionalData(REQUEST_ID_KEY, RID)
                .build();
        listener.onHttpResponse(response);
        assertThat(store.size()).isZero();
    }

    @Test
    void enrichersAreInvokedAndFailuresTolerated() {
        List<String> calls = new ArrayList<>();
        enrichers.add((log, event) -> {
            calls.add("ok");
            log.setAdditionalData(merge(log.getAdditionalData(), "tenant", "acme"));
        });
        enrichers.add((log, event) -> {
            throw new RuntimeException("enricher boom");
        });

        listener.onHttpRequest(requestBuilder().build());

        assertThat(calls).containsExactly("ok");
        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getAdditionalData()).containsEntry("tenant", "acme");
    }

    @Test
    void redactsConfiguredHeadersCaseInsensitively() {
        properties.setRedactHeaders(List.of("x-custom"));
        listener.onHttpRequest(requestBuilder().build());

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getRequestHeaders().get("X-Custom")).isEqualTo("[REDACTED]");
        // Authorization no longer redacted because we overrode the redact list
        assertThat(saved.getRequestHeaders().get("Authorization")).isEqualTo("Bearer secret");
    }

    @Test
    void redactsConfiguredBodyAndAdditionalDataFieldsRecursively() {
        properties.setRedactFields(List.of("password"));

        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("username", "david");
        body.put("password", "p@ss");
        ObjectNode nested = body.putObject("nested");
        nested.put("password", "deep");

        Map<String, Object> extra = new HashMap<>();
        extra.put(REQUEST_ID_KEY, RID);
        extra.put("password", "in-additional-data");

        HttpRequestEvent event = HttpRequestEvent.builder()
                .method(HttpMethodEnum.POST)
                .path("/api/login")
                .headers(new HttpHeaders())
                .additionalData(extra)
                .build();
        event.setBodyPayload(new com.davidrandoll.spring_web_captor.event.BodyPayload(body));

        listener.onHttpRequest(event);

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getRequestBody().get("password").asText()).isEqualTo("[REDACTED]");
        assertThat(saved.getRequestBody().get("username").asText()).isEqualTo("david");
        assertThat(saved.getRequestBody().get("nested").get("password").asText()).isEqualTo("[REDACTED]");
        assertThat(saved.getAdditionalData().get("password")).isEqualTo("[REDACTED]");
    }

    @Test
    void responsePhaseMergesAdditionalDataAcrossPhases() {
        Map<String, Object> reqExtra = new HashMap<>();
        reqExtra.put(REQUEST_ID_KEY, RID);
        reqExtra.put("phase", "request");
        listener.onHttpRequest(requestBuilder().additionalData(reqExtra).build());

        Map<String, Object> resExtra = new HashMap<>();
        resExtra.put(REQUEST_ID_KEY, RID);
        resExtra.put("extra", "response");
        HttpResponseEvent response = HttpResponseEvent.builder()
                .path("/api/things/42")
                .responseStatus(HttpStatus.OK)
                .responseHeaders(new HttpHeaders())
                .additionalData(resExtra)
                .build();
        listener.onHttpResponse(response);

        INetworkLog saved = store.findByRequestId(RID).orElseThrow();
        assertThat(saved.getAdditionalData()).containsEntry("phase", "request");
        assertThat(saved.getAdditionalData()).containsEntry("extra", "response");
    }

    @Test
    void responseWithoutRequestIdIsToleratedOnReject() {
        properties.setCaptureStatuses(List.of(">=500"));
        HttpResponseEvent response = HttpResponseEvent.builder()
                .path("/api/x")
                .responseStatus(HttpStatus.OK)
                .build(); // no requestId in additionalData
        listener.onHttpResponse(response);
        assertThat(store.size()).isZero();
    }

    private static Map<String, Object> merge(Map<String, Object> existing, String k, Object v) {
        Map<String, Object> m = existing == null ? new HashMap<>() : new HashMap<>(existing);
        m.put(k, v);
        return m;
    }
}
