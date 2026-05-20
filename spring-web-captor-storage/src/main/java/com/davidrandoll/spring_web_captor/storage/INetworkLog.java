package com.davidrandoll.spring_web_captor.storage;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;

/**
 * Storage-agnostic shape of a captured HTTP exchange. Consumers implement
 * this on whatever persistence type they prefer — a JPA entity, a Mongo
 * document, a record, an in-memory POJO for tests — and supply an
 * {@link INetworkLogStore} to handle saves/lookups.
 *
 * <p>No identifier method is declared here; how (and whether) a row is
 * identified is the storage backend's concern. The {@link #getRequestId() requestId}
 * pairs the request and response phases of the same exchange.</p>
 *
 * <p>Setters return {@code INetworkLog} so the listener can chain fluently.
 * Implementations using Lombok {@code @Accessors(chain = true)} satisfy
 * this contract automatically via covariant return types.</p>
 */
public interface INetworkLog {

    String getRequestId();
    INetworkLog setRequestId(String value);

    Instant getRequestTimestamp();
    INetworkLog setRequestTimestamp(Instant value);

    String getMethod();
    INetworkLog setMethod(String value);

    String getFullUrl();
    INetworkLog setFullUrl(String value);

    String getPath();
    INetworkLog setPath(String value);

    Map<String, Object> getRequestHeaders();
    INetworkLog setRequestHeaders(Map<String, Object> value);

    Map<String, Object> getQueryParams();
    INetworkLog setQueryParams(Map<String, Object> value);

    Map<String, Object> getPathParams();
    INetworkLog setPathParams(Map<String, Object> value);

    JsonNode getRequestBody();
    INetworkLog setRequestBody(JsonNode value);

    Map<String, Object> getAdditionalData();
    INetworkLog setAdditionalData(Map<String, Object> value);

    Boolean getEndpointCalled();
    INetworkLog setEndpointCalled(Boolean value);

    Integer getResponseStatus();
    INetworkLog setResponseStatus(Integer value);

    Map<String, Object> getResponseHeaders();
    INetworkLog setResponseHeaders(Map<String, Object> value);

    JsonNode getResponseBody();
    INetworkLog setResponseBody(JsonNode value);

    Map<String, Object> getErrorDetail();
    INetworkLog setErrorDetail(Map<String, Object> value);
}
