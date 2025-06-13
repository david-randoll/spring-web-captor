package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.util.Map;

@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class HttpResponseEvent extends BaseHttpEvent {
    private JsonNode responseBody;
    private HttpStatus responseStatus;
    private HttpHeaders responseHeaders;
    private Map<String, Object> errorDetail;

    public HttpResponseEvent(HttpRequestEvent requestEvent) {
        super(requestEvent.toBuilder());
    }

    public void addErrorDetail(@NonNull Map<String, Object> errorDetail) {
        this.errorDetail = errorDetail;
        var factory = new ObjectMapper().getNodeFactory();
        var message = errorDetail.getOrDefault("message", "").toString();
        this.responseBody = factory.textNode(message);
    }

    public boolean isErrorResponse() {
        return responseStatus != null && (isClientErrorResponse() || isServerErrorResponse());
    }

    public boolean isServerErrorResponse() {
        return responseStatus != null && responseStatus.is5xxServerError();
    }

    public boolean isClientErrorResponse() {
        return responseStatus != null && responseStatus.is4xxClientError();
    }

    public boolean isSuccessResponse() {
        return responseStatus != null && responseStatus.is2xxSuccessful();
    }
}