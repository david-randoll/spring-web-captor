package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;

public class RequestEndpointExistsCaptor implements IRequestFieldCaptor {
    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        var requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        var exists = requestWrapper.isEndpointExists();
        builder.endpointExists(exists);
    }
}