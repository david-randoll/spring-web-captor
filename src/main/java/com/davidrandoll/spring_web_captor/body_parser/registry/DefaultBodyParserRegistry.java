package com.davidrandoll.spring_web_captor.body_parser.registry;

import com.davidrandoll.spring_web_captor.body_parser.parsers.FormUrlEncodedBodyParser;
import com.davidrandoll.spring_web_captor.body_parser.parsers.JsonBodyParser;
import com.davidrandoll.spring_web_captor.body_parser.parsers.MultipartBodyParser;
import com.davidrandoll.spring_web_captor.body_parser.parsers.TextBodyParser;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultBodyParserRegistry extends AbstractBodyParserRegistry {
    public DefaultBodyParserRegistry(ObjectMapper objectMapper, WebCaptorProperties.EventDetails properties) {
        register(new JsonBodyParser(objectMapper));
        register(new MultipartBodyParser(properties.isIncludeMultipartFiles()));
        register(new FormUrlEncodedBodyParser());
        register(new TextBodyParser());
    }
}