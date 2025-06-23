package com.davidrandoll.spring_web_captor.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RequestBodyPayload {
    private JsonNode body;
    private MultiValueMap<String, MultipartFile> files;

    public RequestBodyPayload(JsonNode body) {
        this.body = body;
    }

    public RequestBodyPayload(JsonNode body, MultiValueMap<String, MultipartFile> files) {
        this.body = body;
        this.files = files;
    }
}