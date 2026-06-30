package com.davidrandoll.spring_web_captor.storage.testapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorageTestController {

    @GetMapping("/api/widgets/{id}")
    public ResponseEntity<String> widget(@PathVariable String id, @RequestParam(required = false) String color) {
        return ResponseEntity.ok("widget-" + id + "-" + color);
    }
}
