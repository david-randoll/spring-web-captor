package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ResponseErrorCaptor implements IResponseFieldCaptor {
    private final HandlerExceptionResolver resolver;
    private final DefaultErrorAttributes errorAttributes;

    @Override
    public void capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder) {
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.castToCachedBodyHttpServletResponse(response);
        final HttpStatus responseStatus = responseWrapper.getResponseStatus();
        try {
            responseWrapper.getResponseBody()
                    .exceptionally(throwable -> {
                        var ex = new RuntimeException(throwable);
                        responseWrapper.resolveException(ex, resolver);
                        Map<String, Object> errorDetails = getErrorDetails(responseWrapper);
                        builder.addErrorDetail(errorDetails);
                        return null;
                    });
            if (!responseStatus.is2xxSuccessful()) {
                Map<String, Object> errorDetails = getErrorDetails(responseWrapper);
                builder.addErrorDetail(errorDetails);
            }
        } catch (IOException e) {
            log.error("Error getting response body", e);
        }
    }

    public Map<String, Object> getErrorDetails(CachedBodyHttpServletResponse responseWrapper) {
        WebRequest webRequest = new ServletWebRequest(responseWrapper.getRequest());
        ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.values());
        return errorAttributes.getErrorAttributes(webRequest, options);
    }
}