package com.davidrandoll.spring_web_captor.app_property.conditional_path;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/excluded")
class ExcludedTestController {

    @GetMapping
    public ResponseEntity<String> get() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping
    public ResponseEntity<String> post() {
        return ResponseEntity.ok("OK");
    }
}