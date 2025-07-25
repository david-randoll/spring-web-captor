package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.*;
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
    @Setter(AccessLevel.NONE)
    private Map<String, Object> errorDetail;

    public HttpResponseEvent(HttpRequestEvent requestEvent) {
        super(requestEvent.toBuilder());
    }

    public void addErrorDetail(@NonNull Map<String, Object> errorDetail) {
        this.errorDetail = errorDetail;
        var message = errorDetail.getOrDefault("message", "").toString();
        this.responseBody = JsonNodeFactory.instance.textNode(message);
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

    public abstract static class HttpResponseEventBuilder<C extends HttpResponseEvent, B extends HttpResponseEventBuilder<C, B>>
            extends BaseHttpEvent.BaseHttpEventBuilder<C, B> {

        public B addErrorDetail(Map<String, Object> errorDetail) {
            if (errorDetail != null) {
                this.errorDetail(errorDetail);
                var message = errorDetail.getOrDefault("message", "").toString();
                this.responseBody(JsonNodeFactory.instance.textNode(message));
            }
            return self();
        }
    }
}