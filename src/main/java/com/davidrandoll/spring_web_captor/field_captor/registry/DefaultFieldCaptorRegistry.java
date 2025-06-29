package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.field_captor.captors.*;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;

public class DefaultFieldCaptorRegistry extends AbstractFieldCaptorRegistry {
    public DefaultFieldCaptorRegistry(IBodyParserRegistry bodyParserRegistry, WebCaptorProperties.EventDetails properties) {
        if (properties.isIncludeEndpointExists())
            this.register(new RequestEndpointExistsCaptor());
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
        if (properties.isIncludeResponseStatus())
            this.register(new ResponseStatusCaptor());
        if (properties.isIncludeResponseHeaders())
            this.register(new ResponseHeadersCaptor());
        if (properties.isIncludeResponseBody())
            this.register(new ResponseBodyCaptor(bodyParserRegistry));
    }
}