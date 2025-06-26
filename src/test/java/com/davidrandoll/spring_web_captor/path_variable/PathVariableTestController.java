package com.davidrandoll.spring_web_captor.path_variable;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/path")
public class PathVariableTestController {

    @GetMapping("/basic/{id}")
    public ResponseEntity<String> basic(@PathVariable String id) {
        return ResponseEntity.ok("ID: " + id);
    }

    @GetMapping("/multi/{userId}/orders/{orderId}")
    public ResponseEntity<String> multi(@PathVariable String userId, @PathVariable String orderId) {
        return ResponseEntity.ok("User: " + userId + ", Order: " + orderId);
    }

    @GetMapping("/encoded/{name}")
    public ResponseEntity<String> encoded(@PathVariable String name) {
        return ResponseEntity.ok("Name: " + name);
    }

    @GetMapping("/optional/{maybeId}")
    public ResponseEntity<String> optional(@PathVariable(required = false) String maybeId) {
        return ResponseEntity.ok("Maybe ID: " + maybeId);
    }
}