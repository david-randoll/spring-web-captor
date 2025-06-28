package com.davidrandoll.spring_web_captor.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@RequiredArgsConstructor
public class DefaultWebCaptorEventPublisher implements IWebCaptorEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publishEvent(Object event) {
        log.debug("Publishing event: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
}