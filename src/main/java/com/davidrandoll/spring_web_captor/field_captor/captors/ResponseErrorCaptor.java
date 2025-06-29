package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ResponseErrorCaptor implements IResponseFieldCaptor {
    private final HandlerExceptionResolver resolver;
    private final List<IHttpEventExtension> extensions;
    private final IWebCaptorEventPublisher publisher;
    private final DefaultErrorAttributes errorAttributes;

    @Override
    public void capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder) {
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.castToCachedBodyHttpServletResponse(response);
        try {
            responseWrapper.getResponseBody()
                    .exceptionally(throwable -> {
                        var ex = new RuntimeException(throwable);
                        responseWrapper.resolveException(ex, resolver);
                        responseWrapper.publishErrorEvent(extensions, publisher, errorAttributes);
                        return null;
                    });
        } catch (IOException e) {
            log.error("Error getting response body", e);
        }
    }
}