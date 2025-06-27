package com.davidrandoll.spring_web_captor.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class DefaultWebCaptorEventPublisher implements IWebCaptorEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publishEvent(Object event) {
        publisher.publishEvent(event);
    }
}