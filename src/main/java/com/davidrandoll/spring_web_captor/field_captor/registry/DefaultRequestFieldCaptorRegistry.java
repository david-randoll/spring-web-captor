package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.field_captor.captors.*;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;

public class DefaultRequestFieldCaptorRegistry extends AbstractRequestFieldCaptorRegistry {
    public DefaultRequestFieldCaptorRegistry(IBodyParserRegistry bodyParserRegistry, WebCaptorProperties.EventDetails properties) {
        if (properties.isIncludeFullUrl())
            this.register(new RequestFullUrlCaptor());
        if (properties.isIncludePath())
            this.register(new RequestPathCaptor());
        if (properties.isIncludeMethod())
            this.register(new RequestMethodCaptor());
        if (properties.isIncludeRequestHeaders())
            this.register(new RequestHeadersCaptor());
        if (properties.isIncludeQueryParams())
            this.register(new RequestQueryParamsCaptor());
        if (properties.isIncludePathParams())
            this.register(new RequestPathParamsCaptor());
        if (properties.isIncludeRequestBody())
            this.register(new RequestBodyCaptor(bodyParserRegistry));
    }
}