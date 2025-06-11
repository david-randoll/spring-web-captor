package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class HttpRequestEvent {
    private boolean endpointExists;
    private String fullUrl;
    private String path;
    private HttpMethodEnum method;
    private HttpHeaders headers;
    private MultiValueMap<String, String> queryParams;
    private Map<String, String> pathParams;
    private JsonNode requestBody;

    @JsonAnySetter
    @JsonAnyGetter
    private Map<String, Object> additionalData;

    public void addAdditionalData(@NonNull Map<String, Object> additionalData) {
        if (this.additionalData == null) this.additionalData = new HashMap<>();
        this.additionalData.putAll(additionalData);
    }
}