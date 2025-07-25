package com.davidrandoll.spring_web_captor.publisher;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.field_captor.registry.IFieldCaptorRegistry;
import com.davidrandoll.spring_web_captor.publish_conditions.IHttpRequestPublishCondition;
import com.davidrandoll.spring_web_captor.publish_conditions.IHttpResponsePublishCondition;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DefaultHttpEventPublisher implements IHttpEventPublisher {
    private final IWebCaptorEventPublisher publisher;
    private final List<IHttpEventExtension> httpEventExtensions;
    private final IFieldCaptorRegistry registry;
    private final List<IHttpRequestPublishCondition> requestPublishConditions;
    private final List<IHttpResponsePublishCondition> responsePublishConditions;

    @Override
    public void publishRequestEvent(HttpServletRequest request, HttpServletResponse response) {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper);
        if (requestWrapper.isPublished()) return;
        HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent(registry);
        for (IHttpEventExtension extension : httpEventExtensions) {
            try {
                Map<String, Object> additionalData = extension.enrichRequestEvent(requestWrapper, responseWrapper, requestEvent);
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
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper);

        if (responseWrapper.isPublished()) return;

        HttpRequestEvent requestEvent = requestWrapper.toHttpRequestEvent(registry);
        HttpResponseEvent responseEvent = responseWrapper.toHttpResponseEvent(registry);

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

    @Override
    public boolean shouldPublishRequestEvent(HttpServletRequest request, HttpServletResponse response) {
        if (ObjectUtils.isEmpty(requestPublishConditions)) return true;
        return requestPublishConditions.stream()
                .allMatch(condition -> condition.shouldPublishRequest(request, response));
    }

    @Override
    public boolean shouldPublishResponseEvent(HttpServletRequest request, HttpServletResponse response) {
        if (ObjectUtils.isEmpty(responsePublishConditions)) return true;
        return responsePublishConditions.stream()
                .allMatch(condition -> condition.shouldPublishResponse(request, response));
    }
}