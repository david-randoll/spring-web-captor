package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import com.davidrandoll.spring_web_captor.field_captor.IResponseFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.OrderComparator;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractFieldCaptorRegistry implements IFieldCaptorRegistry {
    protected final List<IRequestFieldCaptor> requestCaptors = new ArrayList<>();
    protected final List<IResponseFieldCaptor> responseCaptors = new ArrayList<>();

    @Override
    public void register(IRequestFieldCaptor captor) {
        requestCaptors.add(captor);
    }

    @Override
    public void register(IResponseFieldCaptor captor) {
        responseCaptors.add(captor);
    }

    protected List<IRequestFieldCaptor> getRequestCaptors() {
        return requestCaptors.stream()
                .sorted(OrderComparator.INSTANCE)
                .toList();
    }

    protected List<IResponseFieldCaptor> getResponseCaptors() {
        return responseCaptors.stream()
                .sorted(OrderComparator.INSTANCE)
                .toList();
    }

    @Override
    public HttpRequestEvent.HttpRequestEventBuilder<?, ?> capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        for (IRequestFieldCaptor captor : this.getRequestCaptors()) {
            try {
                captor.capture(request, builder);
            } catch (Exception e) {
                log.error("Error capturing request fields with captor: {}", captor.getClass().getName(), e);
            }
        }
        return builder;
    }

    @Override
    public HttpResponseEvent.HttpResponseEventBuilder<?, ?> capture(HttpServletResponse response, HttpResponseEvent.HttpResponseEventBuilder<?, ?> builder) {
        for (IResponseFieldCaptor captor : this.getResponseCaptors()) {
            try {
                captor.capture(response, builder);
            } catch (Exception e) {
                log.error("Error capturing response fields with captor: {}", captor.getClass().getName(), e);
            }
        }
        return builder;
    }
}