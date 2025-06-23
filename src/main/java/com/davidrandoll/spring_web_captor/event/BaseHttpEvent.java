package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class BaseHttpEvent {
    private boolean endpointExists;
    private String fullUrl;
    private String path;
    private HttpMethodEnum method;
    private HttpHeaders headers;
    private MultiValueMap<String, String> queryParams;
    private Map<String, String> pathParams;
    private RequestBodyPayload requestBodyPayload;

    @JsonAnySetter
    @JsonAnyGetter
    private Map<String, Object> additionalData;

    public void addAdditionalData(@NonNull Map<String, Object> additionalData) {
        if (this.additionalData == null) this.additionalData = new HashMap<>();
        this.additionalData.putAll(additionalData);
    }

    public <T> void addAdditionalData(@NonNull String key, @NonNull T value) {
        if (this.additionalData == null) this.additionalData = new HashMap<>();
        this.additionalData.put(key, value);
    }

    @Nullable
    public <T> T getAdditionalData(String key, Class<T> type) {
        if (this.additionalData == null) return null;
        var value = this.additionalData.get(key);
        if (value == null) return null;
        return type.cast(value);
    }

    @NonNull
    public JsonNode getRequestBody() {
        if (isNull(this.requestBodyPayload))
            return JsonNodeFactory.instance.nullNode();
        return this.requestBodyPayload.getBody();
    }

    @NonNull
    public MultiValueMap<String, MultipartFile> getRequestFiles() {
        if (isNull(this.requestBodyPayload) || isNull(this.requestBodyPayload.getFiles()))
            return MultiValueMap.fromSingleValue(Map.of());
        return this.requestBodyPayload.getFiles();
    }
}
