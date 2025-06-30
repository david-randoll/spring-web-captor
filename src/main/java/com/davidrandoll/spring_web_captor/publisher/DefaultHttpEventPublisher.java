package com.davidrandoll.spring_web_captor.publisher;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DefaultHttpEventPublisher implements IHttpEventPublisher {
    private final IWebCaptorEventPublisher publisher;
    private final List<IHttpEventExtension> httpEventExtensions;

    @Override
    public void publishRequestEvent(HttpServletRequest request, HttpServletResponse response) {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.castToCachedBodyHttpServletRequest(request);
        if (requestWrapper.isPublished()) return;
        HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent();
        for (IHttpEventExtension extension : httpEventExtensions) {
            try {
                Map<String, Object> additionalData = extension.enrichRequestEvent(request, response, requestEvent);
                requestEvent.addAdditionalData(additionalData);
            } catch (Exception e) {
                log.error("Error enriching request event with extension: {}", extension.getClass().getName(), e);
            }
        }
        publisher.publishEvent(requestEvent);
        requestWrapper.markAsPublished();
    }

    @Override
    public void publishResponseEvent(HttpServletRequest request, HttpServletResponse response) {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.castToCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.castToCachedBodyHttpServletResponse(response);

        if (responseWrapper.isPublished()) return;

        HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent();
        HttpResponseEvent responseEvent = responseWrapper.toHttpResponseEvent();

        for (IHttpEventExtension extension : httpEventExtensions) {
            try {
                Map<String, Object> additionalData = extension.enrichResponseEvent(requestWrapper, responseWrapper, requestEvent, responseEvent);
                responseEvent.addAdditionalData(additionalData);
            } catch (Exception e) {
                log.error("Error enriching response event with extension: {}", extension.getClass().getName(), e);
            }
        }
        publisher.publishEvent(responseEvent);
        responseWrapper.markAsPublished();
    }
}