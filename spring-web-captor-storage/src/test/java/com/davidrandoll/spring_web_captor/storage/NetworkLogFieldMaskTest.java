package com.davidrandoll.spring_web_captor.storage;

import com.davidrandoll.spring_web_captor.storage.support.TestNetworkLog;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkLogFieldMaskTest {

    private TestNetworkLog fullyPopulated() {
        TestNetworkLog log = new TestNetworkLog();
        log.setRequestId("rid")
                .setRequestTimestamp(Instant.now())
                .setMethod("GET")
                .setFullUrl("http://x/y")
                .setPath("/y")
                .setEndpointCalled(true)
                .setRequestHeaders(Map.of("h", "v"))
                .setQueryParams(Map.of("q", "1"))
                .setPathParams(Map.of("p", "2"))
                .setRequestBody(new TextNode("req"))
                .setAdditionalData(Map.of("a", "b"))
                .setResponseStatus(200)
                .setResponseHeaders(Map.of("rh", "rv"))
                .setResponseBody(new TextNode("res"))
                .setErrorDetail(Map.of("e", "d"));
        return log;
    }

    @Test
    void nullWhitelistIsNoOp() {
        TestNetworkLog log = fullyPopulated();
        NetworkLogFieldMask.apply(log, null);
        assertThat(log.getPath()).isEqualTo("/y");
        assertThat(log.getResponseBody()).isNotNull();
    }

    @Test
    void emptyWhitelistIsNoOp() {
        TestNetworkLog log = fullyPopulated();
        NetworkLogFieldMask.apply(log, Set.of());
        assertThat(log.getMethod()).isEqualTo("GET");
    }

    @Test
    void nullLogIsTolerated() {
        NetworkLogFieldMask.apply(null, Set.of("path"));
    }

    @Test
    void onlyWhitelistedAndAlwaysKeptFieldsSurvive() {
        TestNetworkLog log = fullyPopulated();
        NetworkLogFieldMask.apply(log, Set.of("path", "responseBody"));

        // whitelisted
        assertThat(log.getPath()).isEqualTo("/y");
        assertThat(log.getResponseBody()).isNotNull();
        // always kept regardless of whitelist
        assertThat(log.getRequestId()).isEqualTo("rid");
        assertThat(log.getRequestTimestamp()).isNotNull();
        assertThat(log.getResponseStatus()).isEqualTo(200);
        // masked away
        assertThat(log.getMethod()).isNull();
        assertThat(log.getFullUrl()).isNull();
        assertThat(log.getEndpointCalled()).isNull();
        assertThat(log.getRequestHeaders()).isNull();
        assertThat(log.getQueryParams()).isNull();
        assertThat(log.getPathParams()).isNull();
        assertThat(log.getRequestBody()).isNull();
        assertThat(log.getAdditionalData()).isNull();
        assertThat(log.getResponseHeaders()).isNull();
        assertThat(log.getErrorDetail()).isNull();
    }
}
