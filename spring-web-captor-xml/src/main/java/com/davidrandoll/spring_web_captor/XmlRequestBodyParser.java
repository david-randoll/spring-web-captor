package com.davidrandoll.spring_web_captor;

import com.davidrandoll.spring_web_captor.body_parser.IRequestBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.ServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Order(1)
public class XmlRequestBodyParser implements IRequestBodyParser {
    private final ObjectMapper xmlMapper;

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("xml");
    }

    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) throws IOException {
        if (ObjectUtils.isEmpty(body)) {
            return new BodyPayload(JsonNodeFactory.instance.nullNode());
        }
        JsonNode node = xmlMapper.readTree(body);
        return new BodyPayload(node);
    }
}