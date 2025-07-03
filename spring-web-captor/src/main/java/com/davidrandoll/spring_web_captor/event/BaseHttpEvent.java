package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
    private BodyPayload bodyPayload;

    @JsonAnySetter
    @JsonAnyGetter
    @Builder.Default
    private Map<String, Object> additionalData = new HashMap<>();

    public void addAdditionalData(@NonNull Map<String, Object> additionalData) {
        this.additionalData.putAll(additionalData);
    }

    public <T> void addAdditionalData(@NonNull String key, @NonNull T value) {
        this.additionalData.put(key, value);
    }

    @Nullable
    public <T> T getAdditionalData(String key, Class<T> type) {
        var value = this.additionalData.get(key);
        if (value == null) return null;
        return type.cast(value);
    }

    public boolean hasAdditionalData(String key) {
        return this.additionalData.containsKey(key);
    }

    public void removeAdditionalData(String key) {
        this.additionalData.remove(key);
    }

    @NonNull
    public JsonNode getRequestBody() {
        if (isNull(this.bodyPayload))
            return JsonNodeFactory.instance.nullNode();
        return this.bodyPayload.getBody();
    }

    @NonNull
    public MultiValueMap<String, MultipartFile> getRequestFiles() {
        if (isNull(this.bodyPayload) || isNull(this.bodyPayload.getFiles()))
            return MultiValueMap.fromSingleValue(Map.of());
        return this.bodyPayload.getFiles();
    }
}
