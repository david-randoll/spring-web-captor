package com.davidrandoll.spring_web_captor.storage.support;

import com.davidrandoll.spring_web_captor.storage.INetworkLog;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Map;

/**
 * Plain in-memory {@link INetworkLog} implementation used by the storage tests. {@code @Accessors(chain = true)}
 * gives covariant chained setters that satisfy the {@code INetworkLog setX(...): INetworkLog} contract — the
 * exact pattern the interface javadoc documents for consumers.
 */
@Data
@Accessors(chain = true)
public class TestNetworkLog implements INetworkLog {
    private String requestId;
    private Instant requestTimestamp;
    private String method;
    private String fullUrl;
    private String path;
    private Map<String, Object> requestHeaders;
    private Map<String, Object> queryParams;
    private Map<String, Object> pathParams;
    private JsonNode requestBody;
    private Map<String, Object> additionalData;
    private Boolean endpointCalled;
    private Integer responseStatus;
    private Map<String, Object> responseHeaders;
    private JsonNode responseBody;
    private Map<String, Object> errorDetail;
}
