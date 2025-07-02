package com.davidrandoll.spring_web_captor.body_parser.parsers;

import com.davidrandoll.spring_web_captor.body_parser.IResponseBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Order(1)
public class JsonResponseBodyParser implements IResponseBodyParser {
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("json");
    }

    @Override
    public BodyPayload parse(HttpServletResponse response, byte[] body) throws IOException {
        if (ObjectUtils.isEmpty(body)) {
            return new BodyPayload(JsonNodeFactory.instance.nullNode());
        }
        return new BodyPayload(objectMapper.readTree(body));
    }
}