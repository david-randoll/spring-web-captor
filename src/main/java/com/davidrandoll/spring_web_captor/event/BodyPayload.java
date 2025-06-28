package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Data
public class BodyPayload {
    private JsonNode body;
    private MultiValueMap<String, MultipartFile> files;

    public BodyPayload(JsonNode body) {
        this.body = body;
    }

    public BodyPayload(JsonNode body, MultiValueMap<String, MultipartFile> files) {
        this.body = body;
        this.files = files;
    }
}