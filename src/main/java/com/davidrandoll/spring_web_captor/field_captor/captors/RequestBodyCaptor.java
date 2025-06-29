package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequestBodyCaptor implements IRequestFieldCaptor {
    private final IBodyParserRegistry bodyParserRegistry;

    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        var requestWrapper = HttpServletUtils.castToCachedBodyHttpServletRequest(request);
        var body = this.getBody(requestWrapper);
        builder.bodyPayload(body);
    }

    public BodyPayload getBody(CachedBodyHttpServletRequest request) {
        byte[] cachedBody = request.getCachedBody();
        return bodyParserRegistry.parse(request.getRequest(), cachedBody);
    }
}