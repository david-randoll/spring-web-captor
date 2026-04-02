package com.davidrandoll.webcaptor.demo.controller;

import com.davidrandoll.webcaptor.demo.listener.CapturedEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/captured-events")
@RequiredArgsConstructor
public class CapturedEventsController {

    private final CapturedEventStore store;

    @GetMapping
    public Map<String, Object> getEvents() {
        return Map.of(
                "requestEvents", store.getRequestEvents(),
                "responseEvents", store.getResponseEvents()
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> clearEvents() {
        store.clear();
        return ResponseEntity.noContent().build();
    }
}
