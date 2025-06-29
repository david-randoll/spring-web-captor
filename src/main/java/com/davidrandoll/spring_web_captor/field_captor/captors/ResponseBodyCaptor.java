package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class ResponseBodyCaptor implements IResponseFieldCaptor {
    private final IBodyParserRegistry bodyParserRegistry;

    @Override
    public void capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder) {
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.castToCachedBodyHttpServletResponse(response);
        final HttpStatus responseStatus = responseWrapper.getResponseStatus();
        try {
            if (responseStatus.is2xxSuccessful()) {
                responseWrapper.getResponseBody()
                        .thenAccept(builder::responseBody);
            }
        } catch (IOException e) {
            log.error("Error getting response body", e);
        }
    }
}