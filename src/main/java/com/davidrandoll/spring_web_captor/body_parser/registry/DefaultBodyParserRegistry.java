package com.davidrandoll.spring_web_captor.body_parser.registry;

import com.davidrandoll.spring_web_captor.body_parser.parsers.*;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class DefaultBodyParserRegistry extends AbstractBodyParserRegistry {
    public DefaultBodyParserRegistry(ObjectMapper objectMapper, XmlMapper xmlMapper, WebCaptorProperties.EventDetails properties) {
        register(new JsonRequestBodyParser(objectMapper));
        register(new JsonResponseBodyParser(objectMapper));

        register(new XmlRequestBodyParser(xmlMapper));
        register(new XmlResponseBodyParser(xmlMapper));

        register(new MultipartRequestBodyParser(properties.isIncludeMultipartFiles()));
        register(new FormUrlEncodedRequestBodyParser());

        register(new TextRequestBodyParser());
        register(new TextResponseBodyParser());
    }
}