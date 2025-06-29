package com.davidrandoll.spring_web_captor.field_captor.registry;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.OrderComparator;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractRequestFieldCaptorRegistry implements IRequestFieldCaptorRegistry {
    protected final List<IRequestFieldCaptor> captors = new ArrayList<>();

    @Override
    public void register(IRequestFieldCaptor captor) {
        captors.add(captor);
    }

    protected List<IRequestFieldCaptor> getCaptors() {
        return captors.stream()
                .sorted(OrderComparator.INSTANCE)
                .toList();
    }

    @Override
    public HttpRequestEvent.HttpRequestEventBuilder<?, ?> capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        for (IRequestFieldCaptor captor : this.getCaptors()) {
            try {
                captor.capture(request, builder);
            } catch (Exception e) {
                log.error("Error capturing request fields with captor: {}", captor.getClass().getName(), e);
            }
        }
        return builder;
    }
}