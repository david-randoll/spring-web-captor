package com.davidrandoll.spring_web_captor.body_parser.parsers;

import com.davidrandoll.spring_web_captor.body_parser.IBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

@Order(1)
public class FormUrlEncodedBodyParser implements IBodyParser {
    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("x-www-form-urlencoded");
    }

    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) throws IOException {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode objectNode = factory.objectNode();
        FormHttpMessageConverter converter = new FormHttpMessageConverter();
        HttpInputMessage inputMessage = toHttpInputMessage(request);
        MultiValueMap<String, String> formData = converter.read(null, inputMessage);
        formData.forEach((key, values) -> {
            if (values.size() == 1) {
                objectNode.put(key, values.getFirst());
            } else {
                ArrayNode arrayNode = factory.arrayNode();
                for (String value : values) {
                    arrayNode.add(value);
                }
                objectNode.set(key, arrayNode);
            }
        });
        return new BodyPayload(objectNode);
    }

    @NonNull
    private static HttpInputMessage toHttpInputMessage(ServletRequest request) {
        return switch (request) {
            case HttpInputMessage httpInputMessage -> httpInputMessage;
            case HttpServletRequest httpServletRequest -> new ServletServerHttpRequest(httpServletRequest);
            default ->
                    throw new IllegalArgumentException("Request must be an instance of HttpServletRequest or HttpInputMessage");
        };
    }
}
