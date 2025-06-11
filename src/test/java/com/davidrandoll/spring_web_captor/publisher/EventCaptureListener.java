package com.davidrandoll.spring_web_captor.publisher;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EventCaptureListener {

    private final List<HttpRequestEvent> requestEvents = new CopyOnWriteArrayList<>();
    private final List<HttpResponseEvent> responseEvents = new CopyOnWriteArrayList<>();

    @EventListener
    public void onRequestEvent(HttpRequestEvent event) {
        requestEvents.add(event);
    }

    @EventListener
    public void onResponseEvent(HttpResponseEvent event) {
        responseEvents.add(event);
    }

    public List<HttpRequestEvent> getRequestEvents() {
        return requestEvents;
    }

    public List<HttpResponseEvent> getResponseEvents() {
        return responseEvents;
    }

    public void clearEvents() {
        requestEvents.clear();
        responseEvents.clear();
    }
}

