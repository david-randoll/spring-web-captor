package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.field_captor.captors.*;

public class DefaultRequestFieldCaptorRegistry extends AbstractRequestFieldCaptorRegistry {
    public DefaultRequestFieldCaptorRegistry(IBodyParserRegistry bodyParserRegistry) {
        this.register(new FullUrlRequestCaptor());
        this.register(new PathRequestCaptor());
        this.register(new MethodRequestCaptor());
        this.register(new HeadersRequestCaptor());
        this.register(new QueryParamsRequestCaptor());
        this.register(new PathParamsRequestCaptor());
        this.register(new BodyRequestCaptor(bodyParserRegistry));
    }
}