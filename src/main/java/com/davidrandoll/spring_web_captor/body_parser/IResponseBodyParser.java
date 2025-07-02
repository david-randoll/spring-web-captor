package com.davidrandoll.spring_web_captor.body_parser;

import com.davidrandoll.spring_web_captor.event.BodyPayload;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface IResponseBodyParser {
    boolean supports(String contentType);

    BodyPayload parse(HttpServletResponse response, byte[] body) throws IOException;
}