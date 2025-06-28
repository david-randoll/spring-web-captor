package com.davidrandoll.spring_web_captor.body_parser.registry;

import com.davidrandoll.spring_web_captor.body_parser.IBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import jakarta.servlet.ServletRequest;

public interface IBodyParserRegistry {
    void register(IBodyParser parser);

    BodyPayload parse(ServletRequest request, byte[] body);
}