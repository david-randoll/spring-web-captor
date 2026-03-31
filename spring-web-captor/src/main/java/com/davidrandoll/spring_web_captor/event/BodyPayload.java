package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class BodyPayload {
    private JsonNode body;

    @JsonIgnore
    private MultiValueMap<String, MultipartFile> files;

    public BodyPayload(JsonNode body) {
        this.body = body;
    }

    public BodyPayload(JsonNode body, MultiValueMap<String, MultipartFile> files) {
        this.body = body;
        this.files = files;
    }

    @JsonProperty("files")
    public Map<String, List<SerializedFile>> getSerializedFiles() {
        if (files == null || files.isEmpty()) return Collections.emptyMap();
        Map<String, List<SerializedFile>> result = new LinkedHashMap<>();
        files.forEach((key, multipartFiles) ->
                result.put(key, multipartFiles.stream().map(SerializedFile::new).toList())
        );
        return result;
    }
}