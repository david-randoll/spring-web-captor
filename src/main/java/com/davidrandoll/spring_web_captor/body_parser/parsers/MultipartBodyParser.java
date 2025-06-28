package com.davidrandoll.spring_web_captor.body_parser.parsers;

import com.davidrandoll.spring_web_captor.body_parser.IBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.IOException;

@Order(1)
public class MultipartBodyParser implements IBodyParser {
    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("multipart");
    }

    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) throws IOException {
        MultipartHttpServletRequest multipartRequest = toMultipartHttpServletRequest(request);

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        multipartRequest.getParameterMap().forEach((key, values) -> {
            if (values.length == 1) {
                objectNode.put(key, values[0]);
            } else {
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                for (String val : values) {
                    arrayNode.add(val);
                }
                objectNode.set(key, arrayNode);
            }
        });

        return new BodyPayload(objectNode, multipartRequest.getMultiFileMap());
    }

    @NonNull
    private static MultipartHttpServletRequest toMultipartHttpServletRequest(ServletRequest request) {
        return switch (request) {
            case MultipartHttpServletRequest multipartHttpServletRequest -> multipartHttpServletRequest;
            case HttpServletRequest httpServletRequest -> new StandardMultipartHttpServletRequest(httpServletRequest);
            default ->
                    throw new IllegalArgumentException("Request must be an instance of HttpServletRequest or MultipartHttpServletRequest");
        };
    }
}
