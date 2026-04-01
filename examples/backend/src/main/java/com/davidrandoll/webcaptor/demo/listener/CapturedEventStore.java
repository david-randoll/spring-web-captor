package com.davidrandoll.webcaptor.demo.listener;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class CapturedEventStore {

    private final List<HttpRequestEvent> requestEvents = new CopyOnWriteArrayList<>();
    private final List<HttpResponseEvent> responseEvents = new CopyOnWriteArrayList<>();

    @EventListener
    public void onRequest(HttpRequestEvent event) {
        requestEvents.add(event);
    }

    @EventListener
    public void onResponse(HttpResponseEvent event) {
        responseEvents.add(event);
    }

    public List<HttpRequestEvent> getRequestEvents() {
        return List.copyOf(requestEvents);
    }

    public List<HttpResponseEvent> getResponseEvents() {
        return List.copyOf(responseEvents);
    }

    public void clear() {
        requestEvents.clear();
        responseEvents.clear();
    }
}
