package com.davidrandoll.spring_web_captor.body_parser.registry;

import com.davidrandoll.spring_web_captor.body_parser.IRequestBodyParser;
import com.davidrandoll.spring_web_captor.body_parser.IResponseBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IBodyParserRegistry {
    void register(IRequestBodyParser parser);
    void register(IResponseBodyParser parser);
    BodyPayload parseRequest(ServletRequest request, byte[] body);
    BodyPayload parseResponse(HttpServletResponse response, byte[] body);
}