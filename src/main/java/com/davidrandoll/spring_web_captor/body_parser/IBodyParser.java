package com.davidrandoll.spring_web_captor.body_parser;

import com.davidrandoll.spring_web_captor.event.BodyPayload;
import jakarta.servlet.ServletRequest;

import java.io.IOException;

public interface IBodyParser {
    boolean supports(String contentType);

    BodyPayload parse(ServletRequest request, byte[] body) throws IOException;
}