package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.field_captor.captors.*;

public class DefaultRequestFieldCaptorRegistry extends AbstractRequestFieldCaptorRegistry {
    public DefaultRequestFieldCaptorRegistry(IBodyParserRegistry bodyParserRegistry) {
        this.register(new RequestFullUrlCaptor());
        this.register(new RequestPathCaptor());
        this.register(new RequestMethodCaptor());
        this.register(new RequestHeadersCaptor());
        this.register(new RequestQueryParamsCaptor());
        this.register(new RequestPathParamsCaptor());
        this.register(new RequestBodyCaptor(bodyParserRegistry));
    }
}