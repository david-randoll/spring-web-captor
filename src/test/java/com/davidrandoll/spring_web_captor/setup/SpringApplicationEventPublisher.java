package com.davidrandoll.spring_web_captor.setup;

import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringApplicationEventPublisher implements IWebCaptorEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publishEvent(Object event) {
        publisher.publishEvent(event);
    }
}